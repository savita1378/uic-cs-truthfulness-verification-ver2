package edu.uic.cs.t_verifier.ml.data;

import weka.core.Instance;

public class ExtendedInstance extends Instance
{
	private static final long serialVersionUID = 1L;

	private Object info;

	public ExtendedInstance(int numAttributes)
	{
		super(numAttributes);
	}

	public ExtendedInstance(ExtendedInstance extendedInstance)
	{
		super(extendedInstance);
	}

	public void setInfo(Object info)
	{
		this.info = info;
	}

	public Object getInfo()
	{
		return info;
	}

	@Override
	public Object copy()
	{
		ExtendedInstance result = new ExtendedInstance(this);
		result.m_Dataset = m_Dataset;
		result.setInfo(info);
		return result;

	}

	@Override
	public String toString()
	{
		return super.toString() + " | " + info.toString();
	}

}
