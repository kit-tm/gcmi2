package com.dgeiger.enhanced_framework.filtering;

import com.dgeiger.enhanced_framework.apps.App;

import java.util.ArrayList;

public interface FilterApp extends App {

    ArrayList<Filter> getFilters();
}
