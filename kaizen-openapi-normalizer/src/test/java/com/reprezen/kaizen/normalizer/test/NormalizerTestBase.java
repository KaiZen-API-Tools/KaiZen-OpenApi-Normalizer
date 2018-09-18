package com.reprezen.kaizen.normalizer.test;

import java.io.IOException;
import java.net.URL;

import org.junit.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class NormalizerTestBase extends Assert {

	protected static ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

	protected URL getYamlFileUrl(String name) {
		return getClass().getResource(String.format("/models/%s.yaml", name));
	}

	protected JsonNode loadYaml(String name) throws JsonProcessingException, IOException {
		return yamlMapper.readTree(getYamlFileUrl(name));
	}
}
