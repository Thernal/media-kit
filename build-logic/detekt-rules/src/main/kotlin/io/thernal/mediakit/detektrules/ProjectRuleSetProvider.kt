package io.thernal.mediakit.detektrules

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider
import io.thernal.mediakit.detektrules.collections.UnsafeCollectionIndexAccess
import io.thernal.mediakit.detektrules.packageboundary.LayerPackageBoundary
import io.thernal.mediakit.detektrules.packageboundary.LayerPackageRequired
import io.thernal.mediakit.detektrules.preview.PreviewMustBePrivate
import io.thernal.mediakit.detektrules.style.ExpressionBodyNotAllowed
import io.thernal.mediakit.detektrules.style.MultilineConstructorRequired

class ProjectRuleSetProvider : RuleSetProvider {
    override val ruleSetId = RuleSetId("project")

    override fun instance() = RuleSet(
        ruleSetId,
        listOf(
            ::PreviewMustBePrivate,
            ::UnsafeCollectionIndexAccess,
            ::LayerPackageBoundary,
            ::LayerPackageRequired,
            ::ExpressionBodyNotAllowed,
            ::MultilineConstructorRequired,
        ),
    )
}
