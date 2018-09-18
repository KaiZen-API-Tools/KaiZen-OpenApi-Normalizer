package com.reprezen.kaizen.normalizer;

import static com.reprezen.kaizen.normalizer.Reference.getRefString;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.reprezen.kaizen.normalizer.Localizer.LocalizedContent;
import com.reprezen.kaizen.normalizer.Reference.ReferenceTreatment;
import com.reprezen.kaizen.normalizer.util.JsonStateWalker;
import com.reprezen.kaizen.normalizer.util.JsonStateWalker.AdvancedWalkMethod;
import com.reprezen.kaizen.normalizer.util.JsonStateWalker.Disposition;
import com.reprezen.kaizen.normalizer.util.StateMachine;
import com.reprezen.kaizen.normalizer.util.StateMachine.State;
import com.reprezen.kaizen.normalizer.util.StateMachine.Tracker;

/**
 * This class scans json structures for references and handles them as needed.
 * 
 * @author Andy Lowry
 *
 */
public class ReferenceScanner<E extends Enum<E> & Component> {
	private StateMachine<E> machine;
	private JsonNode tree;
	private Reference base;
	private ScanOp scanOp;
	private ContentManager<E> contentManager;
	private Options options;

	public ReferenceScanner(JsonNode tree, Reference base, ScanOp scanOp, ContentManager<E> contentManager,
			Options options) {
		this.tree = tree;
		this.base = base;
		this.scanOp = scanOp;
		this.contentManager = contentManager;
		this.options = options;
		this.machine = contentManager.getMachine();
	}

	public JsonNode scan() {
		return scan(machine.getState("MODEL"));
	}

	public JsonNode scan(E start) {
		return scan(machine.getState(start));
	}

	public JsonNode scan(State<E> startState) {
		Tracker<E> tracker = machine.tracker(startState);
		Walkers<E> walkers = new Walkers<E>(base, contentManager, options);
		AdvancedWalkMethod<E> walkMethod = walkers.getWalkMethod(scanOp);
		Optional<JsonNode> newNode = new JsonStateWalker<E>(tracker, walkMethod).walk(tree);
		return newNode.orElse(tree);
	}

	private static class Walkers<E extends Enum<E> & Component> {
		private Reference base;
		private ContentManager<E> contentManager;
		private Options options;

		public Walkers(Reference base, ContentManager<E> contentManager, Options options) {
			this.base = base;
			this.contentManager = contentManager;
			this.options = options;
		}

		public AdvancedWalkMethod<E> getWalkMethod(ScanOp scanOp) {
			switch (scanOp) {
			case LOAD:
				return this::loadWalkMethod;
			case COMPONENTS:
				return this::componentWalkMethod;
			case POLICY:
				return this::policyWalkMethod;
			case NONE:
			default:
				throw new IllegalArgumentException("There is no walk method for a NONE scan oepration");
			}
		}

		/**
		 * Walk method for the LOAD phase, where model files are filled in by having
		 * their non-conforming references inlined.
		 * <p>
		 * Scan Args:
		 * <ol>
		 * <li>boolean - whether to fix simple refs</li>
		 * </ol>
		 *
		 */
		public Disposition loadWalkMethod(JsonNode node, State<E> state, E stateValue, List<Object> path,
				JsonPointer pointer) {
			if (Reference.isRefNode(node)) {
				Reference ref = new Reference(getRefString(node).get(), base, stateValue);
				if (options.isRewriteSimpleRefs()) {
					ref.rewriteSimpleRef();
				}
				switch (ref.getTreatment(options)) {
				case INLINE_NONCONFORMING: {
					// inline and re-walk non-conforming ref, but if we can't load it, replace it
					// with an adorned ref node
					// TODO handle cycles
					Content<E> toInline = contentManager.load(ref, state);
					return toInline.isValid() ? Disposition.rewalk(toInline.copyTree())
							: Disposition.done(toInline.getRef().getRefNode());
				}
				case MERGE:
				case INLINE_CONFORMING:
				case LOCALIZE:
				case RETAIN:
				case ERROR:
					// all other refs are copied with adornments
					return Disposition.done(ref.getRefNode());
				}
			}
			return Disposition.normal();
		}

		/**
		 * Walk method for the COMPONENTS phase, where local component definitions are
		 * located and added to the localizer for possible inclusion in the final model.
		 * <p>
		 * Name collisions are resolved by the localizer. Note that additional component
		 * definitions may be localized in the POLICY phase, since COMPONENTS phase only
		 * localizes definitions in their standard containers in top-level source
		 * models.
		 * <p>
		 * Scan args: none
		 */
		public Disposition componentWalkMethod(JsonNode node, State<E> state, E stateValue, List<Object> path,
				JsonPointer pointer) {
			if (stateValue.isDefiningSite()) {
				if (stateValue.hasMergeSemantics()) {
					// this is for paths - the definition may or may not have a reference. If it
					// does, it may have other fields. In all cases, we take the defining value,
					// remove a reference string and its adornments, if they're present, and
					// localize the rest (which may be empty). That establishes the localized path.
					// The reference, if any, will be merged during Policy phase.
					JsonNode copy = node.deepCopy();
					if (copy.has("$ref")) {
						((ObjectNode) copy).remove("$ref");
						if (copy.has(Reference.ADORNMENT_PROPERTY)) {
							((ObjectNode) copy).remove(Reference.ADORNMENT_PROPERTY);
						}
					}
					contentManager.localize(copy, stateValue.getDefinedComponent(), pointer, base);

				} else {
					contentManager.localize(node, stateValue.getDefinedComponent(), pointer, base);
				}
			}
			return Disposition.normal();
		}

		/**
		 * Walk method for the POLICY phase, where conforming references are either
		 * inlined or retained, accorinding to retention policy in force.
		 * <p>
		 * Conforming references are resolved for the first time in this phase, and such
		 * references may reach into files that have not previously been loaded at all.
		 * Therefore, the resolved content may include additional conforming and
		 * non-conforming references.
		 * <p>
		 * There should not be any non-conforming references left in the top-level
		 * models at this point, but we don't verify that. We just inline and re-walk
		 * all nonconforming references.
		 * <p>
		 * Note that when loading new content in this phase, we perform a LOAD phase
		 * scan on that content to inline nonconforming content, but there's no need to
		 * do a COMPONENT phase scan. This is because only top-level model files
		 * (completed with their non-conforming references) can contribute their own
		 * component definitions to the normalized model. Of course conforming
		 * references contained in newly loaded content may still contribute new
		 * definitions through localization.
		 * <p>
		 * Scan args:
		 * <ol>
		 * <li>boolean - whether to fix simple refs when scanning new content inlined or
		 * localized content</li>
		 */
		public Disposition policyWalkMethod(JsonNode node, State<E> state, E stateValue, List<Object> path,
				JsonPointer pointer) {
			if (Reference.isRefNode(node) && stateValue.isConformingSite()) {
				Reference ref = new Reference(getRefString(node).get(), base, stateValue);
				ReferenceTreatment treatment = ref.getTreatment(options);
				switch (treatment) {
				case INLINE_NONCONFORMING: {
					// same behavior as in the LOAD phase walk method
					// TODO handle cycles
					Content<E> toInline = contentManager.load(ref, state);
					toInline.scan(ScanOp.LOAD);
					return toInline.isValid() ? Disposition.rewalk(toInline.copyTree())
							: Disposition.done(toInline.getRef().getRefNode(false));
				}
				case INLINE_CONFORMING: {
					// almost the same, but if this reference creates a cycle, we localize it
					// instead of throwing an exception
					// TODO handle cycles
					Content<E> toInline = contentManager.load(ref, state);
					if (toInline.isValid()) {
						toInline.scan(ScanOp.LOAD);
					}
					return toInline.isValid() ? Disposition.rewalk(toInline.copyTree())
							: Disposition.done(toInline.getRef().getRefNode(false));
				}
				case LOCALIZE: {
					Content<E> toLocalize = contentManager.load(ref, state);
					if (toLocalize.isValid()) {
						toLocalize.scan(ScanOp.LOAD);
						toLocalize.scan(ScanOp.POLICY);
						LocalizedContent localized = contentManager.localize(toLocalize.getTree(), stateValue,
								ref.getPointer(), ref);
						return Disposition.done(localized.getLocalizedRef(ref).getRefNode(false));
					} else {
						return Disposition.done(ref.getRefNode(false));
					}
				}
				case MERGE: {
					// here's where we actually handle the reference in a path item, having
					// localized the path's non-ref content previously.
					Content<E> toMerge = contentManager.load(ref, state);
					if (toMerge.isValid()) {
						toMerge.scan(ScanOp.LOAD);
						toMerge.scan(ScanOp.POLICY);
						contentManager.mergeLocalize(toMerge.getTree(), stateValue, pointer, base);
					} else {
						contentManager.mergeLocalize(ref.getRefNode(false), stateValue, pointer, base);
					}
					return Disposition.done(ref.getRefNode(false));
				}
				case ERROR:
				case RETAIN:
					// pass through erroneous and retained references, but remove any adornments
					// from prior phases
					return Disposition.done(ref.getRefNode(false));
				default:
					break;

				}
			}
			return Disposition.normal();
		}
	}

	public enum ScanOp {
		NONE, //
		/**
		 * Initial load - recursively scan for references and create Reference objects
		 * for all. Inline content for all non-conforming references.
		 */
		LOAD, //
		/**
		 * Scan for all local components and mark them for retention as dictated by
		 * policy
		 */
		COMPONENTS, //
		/**
		 * Scan for valid references and either inline or retain them per policy.
		 * Nonconforming references should not be present during this phase. Any that
		 * are encountered are makred as invalid.
		 */
		POLICY;
	}
}