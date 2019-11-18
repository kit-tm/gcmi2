package com.dgeiger.enhanced_framework.filtering;

public class FilterCondition<T, E extends Enum> {

    private E filterCriterion;
    private T value;

    public FilterCondition(E filterCriterion, T value) {
        this.filterCriterion = filterCriterion;
        this.value = value;
    }

    public E getFilterCriterion() {
        return filterCriterion;
    }

    public void setFilterCriterion(E filterCriterion) {
        this.filterCriterion = filterCriterion;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
