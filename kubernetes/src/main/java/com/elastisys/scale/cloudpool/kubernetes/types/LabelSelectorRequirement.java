package com.elastisys.scale.cloudpool.kubernetes.types;

import static com.elastisys.scale.cloudpool.kubernetes.types.Operator.DoesNotExist;
import static com.elastisys.scale.cloudpool.kubernetes.types.Operator.Exists;
import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;
import static java.util.Arrays.asList;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * See
 * https://kubernetes.io/docs/api-reference/v1.5/#labelselectorrequirement-unversioned
 *
 */
public class LabelSelectorRequirement {
    public String key;
    public Operator operator;
    public List<String> values;

    /**
     * Creates a {@code label in (value1, value2, ...)} type of
     * {@link LabelSelectorRequirement}.
     *
     * @param label
     * @param acceptableValues
     * @return
     */
    public static LabelSelectorRequirement in(String label, String... acceptableValues) {
        checkArgument(acceptableValues != null, "acceptableValues must no be null");
        checkArgument(acceptableValues.length > 0, "at least one acceptableValue must be given");
        LabelSelectorRequirement req = new LabelSelectorRequirement();
        req.key = label;
        req.operator = Operator.In;
        req.values = Arrays.asList(acceptableValues);
        return req;
    }

    /**
     * Creates a {@code label notin (value1, value2, ...)} type of
     * {@link LabelSelectorRequirement}.
     *
     * @param label
     * @param unacceptableValues
     * @return
     */
    public static LabelSelectorRequirement notin(String label, String... unacceptableValues) {
        checkArgument(unacceptableValues != null, "unacceptableValues must no be null");
        checkArgument(unacceptableValues.length > 0, "at least one unacceptableValue must be given");
        LabelSelectorRequirement req = new LabelSelectorRequirement();
        req.key = label;
        req.operator = Operator.NotIn;
        req.values = Arrays.asList(unacceptableValues);
        return req;
    }

    /**
     * Creates a {@code label} type of {@link LabelSelectorRequirement}.
     *
     * @param label
     * @return
     */
    public static LabelSelectorRequirement exists(String label) {
        LabelSelectorRequirement req = new LabelSelectorRequirement();
        req.key = label;
        req.operator = Operator.Exists;
        return req;
    }

    /**
     * Creates a {@code !label} type of {@link LabelSelectorRequirement}.
     *
     * @param label
     * @return
     */
    public static LabelSelectorRequirement notexists(String label) {
        LabelSelectorRequirement req = new LabelSelectorRequirement();
        req.key = label;
        req.operator = Operator.DoesNotExist;
        return req;
    }

    /**
     * Converts this {@link LabelSelectorRequirement} to a {@link String} that
     * can be used in a <a href=
     * "https://kubernetes.io/docs/user-guide/labels/#label-selectors">label
     * selector</a> expression, for example when listing {@link Pod}s.
     *
     * @return
     */
    public String toLabelSelector() {
        validate();
        switch (this.operator) {
        case In:
            return this.key + " in (" + String.join(",", this.values) + ")";
        case NotIn:
            return this.key + " notin (" + String.join(",", this.values) + ")";
        case DoesNotExist:
            return "!" + this.key;
        case Exists:
            return this.key;
        default:
            throw new IllegalArgumentException("unrecognized operator: " + this.operator);
        }
    }

    /**
     * Validates this {@link LabelSelectorRequirement}. See
     * https://kubernetes.io/docs/api-reference/v1.5/#labelselectorrequirement-unversioned
     */
    public void validate() {
        checkArgument(this.key != null, "matchExpression: missing key");
        checkArgument(this.operator != null, "matchExpression: missing operatror");
        if (asList(DoesNotExist, Exists).contains(this.operator)) {
            checkArgument(this.values == null,
                    "matchExpression: no values can be specified when using operators Exists or DoesNotExist");
        } else if (asList(Operator.In, Operator.NotIn).contains(this.operator)) {
            checkArgument(this.values != null, "matchExpression: In and NotIn operators require values");
        } else {
            throw new IllegalArgumentException("matchExpression: unrecognized operator: " + this.operator);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.key, this.operator, this.values);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LabelSelectorRequirement) {
            LabelSelectorRequirement that = (LabelSelectorRequirement) obj;
            return Objects.equals(this.key, that.key) //
                    && Objects.equals(this.operator, that.operator) //
                    && Objects.equals(this.values, that.values);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

}
