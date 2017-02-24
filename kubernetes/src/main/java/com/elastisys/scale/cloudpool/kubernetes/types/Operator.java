package com.elastisys.scale.cloudpool.kubernetes.types;

/**
 * Allowed operators in a {@link LabelSelectorRequirement}.
 *
 * @see LabelSelectorRequirement
 */
public enum Operator {

    In, NotIn, Exists, DoesNotExist;
}
