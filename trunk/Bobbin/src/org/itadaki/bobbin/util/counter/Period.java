package org.itadaki.bobbin.util.counter;


/**
 * A period consisting of a number of time units of equal length
 */
public final class Period {

	/**
	 * The length of the time unit of which the period is composed
	 */
	private final int unitLength;

	/**
	 * The total number of time units contained within the period
	 */
	private final int totalUnits;


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + this.totalUnits;
		result = prime * result + this.unitLength;
		return result;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals (Object other) {

		if (this == other)
			return true;

		if ((other == null) || (getClass() != other.getClass()))
			return false;

		Period otherPeriod = (Period) other;

		if ((this.totalUnits == otherPeriod.totalUnits) && (this.unitLength == otherPeriod.unitLength))
			return true;

		return false;

	}


	/**
	 * @return The length of the time unit of which the period is composed
	 */
	public int getUnitLength() {

		return this.unitLength;

	}


	/**
	 * @return The total number of time units contained within the period
	 */
	public int getTotalUnits() {

		return this.totalUnits;

	}


	/**
	 * @param unitLength The length in milliseconds of the time unit of which the period is composed
	 * @param totalUnits The total number of time units contained within the period
	 */
	public Period (int unitLength, int totalUnits) {

		this.unitLength = unitLength;
		this.totalUnits = totalUnits;

	}

}
