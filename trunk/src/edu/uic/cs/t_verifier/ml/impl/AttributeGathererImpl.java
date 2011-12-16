package edu.uic.cs.t_verifier.ml.impl;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import edu.uic.cs.t_verifier.misc.Assert;
import edu.uic.cs.t_verifier.ml.AttributeGatherer;

public class AttributeGathererImpl implements AttributeGatherer
{
	private static final String TARGET_ATTRIBUTE_NAME = "TARGET";
	private static final String DATA_SET_NAME = "DATA_SET";

	private Map<String, Attribute> attributeDefinitionByName = new LinkedHashMap<String, Attribute>();
	private LinkedHashMap<Object, Map<String, Object>> attributeValuesById = new LinkedHashMap<Object, Map<String, Object>>();

	@Override
	public void addAttribute(Object recordId, String attributeName,
			String arrtibuteValue, String[] attributeDomain)
	{
		if (!attributeDefinitionByName.containsKey(attributeName))
		{
			FastVector attributeDomainVector = new FastVector(
					attributeDomain.length);
			for (String domainValue : attributeDomain)
			{
				attributeDomainVector.addElement(domainValue);
			}
			attributeDefinitionByName.put(attributeName, new Attribute(
					attributeName, attributeDomainVector));
		}

		storeAttributeValue(recordId, attributeName, arrtibuteValue);

	}

	private void storeAttributeValue(Object recordId, String attributeName,
			Object arrtibuteValue)
	{
		Map<String, Object> attributeValuesByName = attributeValuesById
				.get(recordId);
		if (attributeValuesByName == null)
		{
			attributeValuesByName = new HashMap<String, Object>();
			attributeValuesById.put(recordId, attributeValuesByName);
		}

		Assert.isTrue(
				attributeValuesByName.put(attributeName, arrtibuteValue) == null,
				"Value for attribute[" + recordId + " : " + attributeName
						+ "] already exists! ");
	}

	@Override
	public void addAttribute(Object recordId, String attributeName,
			double arrtibuteValue)
	{
		if (!attributeDefinitionByName.containsKey(attributeName))
		{
			attributeDefinitionByName.put(attributeName, new Attribute(
					attributeName));
		}

		storeAttributeValue(recordId, attributeName,
				Double.valueOf(arrtibuteValue));
	}

	@Override
	public void addAttribute(Object recordId, String attributeName,
			int arrtibuteValue)
	{
		addAttribute(recordId, attributeName, (double) arrtibuteValue);

	}

	@Override
	public Instances getDataset(Map<?, ?> targetValueByKey,
			String[] targetDomain)
	{
		int numAttributes = attributeDefinitionByName.size() + 1;
		FastVector allAttributeDefinitions = new FastVector(numAttributes); // plus 1 for targetValue
		for (Attribute attributeDef : attributeDefinitionByName.values())
		{
			allAttributeDefinitions.addElement(attributeDef);
		}

		Attribute targetAttributeDef = null;
		if (targetDomain != null && targetDomain.length != 0)
		{
			FastVector targetDomainVector = new FastVector(targetDomain.length);
			for (String domainValue : targetDomain)
			{
				targetDomainVector.addElement(domainValue);
			}

			targetAttributeDef = new Attribute(TARGET_ATTRIBUTE_NAME,
					targetDomainVector);
		}
		else
		{
			targetAttributeDef = new Attribute(TARGET_ATTRIBUTE_NAME);
		}
		allAttributeDefinitions.addElement(targetAttributeDef);

		////////////////////////////////////////////////////////////////////////
		Instances dataset = new Instances(DATA_SET_NAME,
				allAttributeDefinitions, attributeValuesById.size());
		for (Object id : attributeValuesById.keySet())
		{
			Instance instance = new Instance(numAttributes);
			instance.setDataset(dataset);

			Map<String, Object> attributeValueByName = attributeValuesById
					.get(id);
			// for all attributes we have
			for (Entry<String, Attribute> entry : attributeDefinitionByName
					.entrySet())
			{
				String attributeName = entry.getKey();
				Attribute attributeDef = entry.getValue();

				Object attributeValue = attributeValueByName.get(attributeName);
				if (attributeValue == null)
				{
					// one record don't have such attribute
					instance.setMissing(attributeDef);
				}
				else if (attributeValue instanceof String)
				{
					instance.setValue(attributeDef, (String) attributeValue);
				}
				else
				{
					instance.setValue(attributeDef,
							((Double) attributeValue).doubleValue());
				}

			}

			Object targetValue = targetValueByKey.get(id);
			Assert.notNull(targetValue);
			if (targetValue instanceof String)
			{
				instance.setValue(targetAttributeDef, (String) targetValue);
			}
			else
			{
				instance.setValue(targetAttributeDef,
						((Double) targetValue).doubleValue());
			}

			dataset.add(instance);

		}

		dataset.setClass(targetAttributeDef);

		return dataset;
	}
}
