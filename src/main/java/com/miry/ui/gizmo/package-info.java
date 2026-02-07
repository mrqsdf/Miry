/**
 * Viewport-independent editor gizmos (translate/rotate/scale building blocks).
 * <p>
 * Gizmos in this package operate on math inputs (camera, view-projection matrix, viewport
 * rectangle, mouse) and produce simple geometry (triangles) that the host can render using its
 * preferred 3D backend. This makes them usable in other engines (including Minecraft mod UIs)
 * without depending on a specific rendering API.
 */
package com.miry.ui.gizmo;

