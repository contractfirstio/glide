package glide.ui.peoplegroup

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import glide.ui.layout.GlideLayout
import glide.ui.shared.FormPanelActionSubsection
import glide.ui.shared.FormPanelLinkedBox
import glide.ui.shared.FormPanelSection
import glide.ui.shared.FormPanelSectionRole
import glide.ui.shared.FormPanelSectionsDivider
import glide.ui.shared.FormPanelSummaryCard

typealias LeadPanelSectionRole = FormPanelSectionRole

@Composable
fun LeadPeopleSectionsDivider(
    spacing: GlideLayout.Spacing,
    modifier: Modifier = Modifier,
) = FormPanelSectionsDivider(
    label = "Others on this lead",
    spacing = spacing,
    modifier = modifier,
)

@Composable
fun LeadPanelSection(
    title: String,
    description: String,
    spacing: GlideLayout.Spacing,
    role: LeadPanelSectionRole,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) = FormPanelSection(
    title = title,
    description = description,
    spacing = spacing,
    role = role,
    modifier = modifier,
    content = content,
)

@Composable
fun LeadMainClientSummaryCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) = FormPanelSummaryCard(role = FormPanelSectionRole.Primary, modifier = modifier, content = content)

@Composable
fun LeadRelatedPeopleLinkedBox(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) = FormPanelLinkedBox(role = FormPanelSectionRole.Secondary, modifier = modifier, content = content)

@Composable
fun LeadActionSubsection(
    title: String,
    description: String,
    spacing: GlideLayout.Spacing,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) = FormPanelActionSubsection(
    title = title,
    description = description,
    spacing = spacing,
    modifier = modifier,
    content = content,
)
