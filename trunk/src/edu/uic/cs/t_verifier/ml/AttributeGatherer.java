package edu.uic.cs.t_verifier.ml;

import java.util.Map;

import weka.core.Instances;

public interface AttributeGatherer
{
	AttributeGatherer DummyAttributeGatherer = new AttributeGatherer()
	{
		@Override
		public void addAttribute(Object recordId, String attributeName,
				String arrtibuteValue, String[] attributeDomain)
		{
		}

		@Override
		public void addAttribute(Object recordId, String attributeName,
				double arrtibuteValue)
		{
		}

		@Override
		public void addAttribute(Object recordId, String attributeName,
				int arrtibuteValue)
		{
		}

		@Override
		public Instances getDataset(Map<?, ?> targetValueByKey,
				String[] targetDomain, Map<String, String> unsignedValueByName)
		{
			return null;
		}
	};

	void addAttribute(Object recordId, String attributeName,
			String arrtibuteValue, String[] attributeDomain);

	void addAttribute(Object recordId, String attributeName,
			double arrtibuteValue);

	void addAttribute(Object recordId, String attributeName, int arrtibuteValue);

	Instances getDataset(Map<?, ?> targetValueByKey, String[] targetDomain,
			Map<String, String> unsignedValueByName);
}
