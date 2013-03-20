package org.motechproject.tasks.domain;

import org.codehaus.jackson.annotate.JsonIgnore;

import java.io.Serializable;
import java.util.Objects;

public class TaskAdditionalData implements Serializable {
    private static final long serialVersionUID = 6652124746431496660L;

    private Long id;
    private String type;
    private String lookupField;
    private String lookupValue;
    private boolean fail;

    public TaskAdditionalData() {
        this(null, null, null, null, false);
    }

    public TaskAdditionalData(Long id, String type, String lookupField, String lookupValue, boolean fail) {
        this.id = id;
        this.type = type;
        this.lookupField = lookupField;
        this.lookupValue = lookupValue;
        this.fail = fail;
    }

    @JsonIgnore
    public boolean objectEquals(Long id, String type) {
        return Objects.equals(this.id, id) && Objects.equals(this.type, type);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLookupField() {
        return lookupField;
    }

    public void setLookupField(String lookupField) {
        this.lookupField = lookupField;
    }

    public String getLookupValue() {
        return lookupValue;
    }

    public void setLookupValue(String lookupValue) {
        this.lookupValue = lookupValue;
    }

    public boolean isFail() {
        return fail;
    }

    public void setFail(boolean fail) {
        this.fail = fail;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, lookupField, lookupValue, fail);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final TaskAdditionalData other = (TaskAdditionalData) obj;

        return objectEquals(other.id, other.type) &&
                Objects.equals(this.lookupField, other.lookupField) &&
                Objects.equals(this.lookupValue, other.lookupValue);
    }

    @Override
    public String toString() {
        return String.format("TaskAdditionalData{id=%d, type='%s', lookupField='%s', lookupValue='%s', fail='%s'}",
                id, type, lookupField, lookupValue, fail);
    }
}
