package com.miry.ui.nodegraph;

/**
 * A directed connection between an output pin and an input pin.
 */
public record NodeConnection(int id, NodePin from, NodePin to) {}
