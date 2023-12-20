package com.squareup.tooling.models

import java.io.Serializable

public interface SquareProjectParameters : Serializable {
    public var gitRoot: String

    public companion object {
        private const val serialVersionUid: Long = 1L
    }
}
