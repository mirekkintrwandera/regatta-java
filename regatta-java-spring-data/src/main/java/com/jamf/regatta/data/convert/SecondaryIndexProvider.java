package com.jamf.regatta.data.convert;

import java.util.List;

public interface SecondaryIndexProvider {

	IndexEntry primaryKey();
	List<IndexEntry> secondaryIndexes();
}
