package com.theundertaker11.mtqoladditions.utils;

import lordfokas.mineraltracker.tracker.IDeposit;

public class DepositDistanceObj implements Comparable<DepositDistanceObj>{
	
	public final IDeposit deposit;
	public final int distance;
	
	public DepositDistanceObj(IDeposit deposit, int distance) {
		this.deposit = deposit;
		this.distance = distance;
	}

	@Override
	public int compareTo(DepositDistanceObj o) {
		return this.distance - o.distance;
	}
}
