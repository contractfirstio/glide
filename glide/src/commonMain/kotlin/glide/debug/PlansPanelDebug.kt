package glide.debug

/** @deprecated Use [GlidePanelDebug] with [GlidePanelDebug.Panel.PLANS]. */
@Deprecated("Use GlidePanelDebug", ReplaceWith("GlidePanelDebug"))
object PlansPanelDebug {
    fun log(event: String, detail: String = "") =
        GlidePanelDebug.log(GlidePanelDebug.Panel.PLANS, event, detail)
}
