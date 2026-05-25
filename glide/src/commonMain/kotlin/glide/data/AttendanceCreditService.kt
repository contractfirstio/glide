package glide.data

import glide.model.AttendanceStatus
import glide.model.AttendanceAttendee
import glide.model.AttendanceSessionKey
import glide.model.DEFAULT_CURRENCY_CODE
import glide.model.formatMoney

data class AbsentCreditPreview(
    val attendee: AttendanceAttendee,
    val amountMinor: Long,
    val currencyCode: String,
    val eligible: Boolean,
    val alreadyCredited: Boolean,
)

data class AttendanceCreditResult(
    val creditsAdded: Int,
    val creditsSkipped: Int,
    val totalCreditMinor: Long,
    val currencyCode: String,
)

object AttendanceCreditService {
    fun absentAttendees(
        attendees: List<AttendanceAttendee>,
        statusByAttendeeKey: Map<String, AttendanceStatus?>,
    ): List<AttendanceAttendee> = attendees.filter { statusByAttendeeKey[it.key] == AttendanceStatus.ABSENT }

    fun previewCredits(
        session: AttendanceSessionKey,
        absentAttendees: List<AttendanceAttendee>,
    ): List<AbsentCreditPreview> = absentAttendees.map { attendee ->
        val enrollment = SoldPlanEnrollmentStore.forSoldPlan(attendee.soldPlanId)
        val snapshot = enrollment?.planSnapshot
        val amountMinor = snapshot?.perSessionCreditPerPersonMinor() ?: 0L
        val currencyCode = snapshot?.currencyCode ?: DEFAULT_CURRENCY_FALLBACK
        val alreadyCredited = AttendanceCreditStore.hasCreditForAbsentSession(
            session.classId,
            session.sessionDate,
            attendee.key,
        )
        AbsentCreditPreview(
            attendee = attendee,
            amountMinor = amountMinor,
            currencyCode = currencyCode,
            eligible = enrollment != null && amountMinor > 0,
            alreadyCredited = alreadyCredited,
        )
    }

    fun applyCreditsForAbsentAttendees(
        session: AttendanceSessionKey,
        className: String,
        absentAttendees: List<AttendanceAttendee>,
    ): AttendanceCreditResult {
        var creditsAdded = 0
        var creditsSkipped = 0
        var totalCreditMinor = 0L
        var currencyCode = DEFAULT_CURRENCY_FALLBACK

        absentAttendees.forEach { attendee ->
            val enrollment = SoldPlanEnrollmentStore.forSoldPlan(attendee.soldPlanId)
            if (enrollment == null) {
                creditsSkipped++
                return@forEach
            }
            val snapshot = enrollment.planSnapshot
            val amountMinor = snapshot.perSessionCreditPerPersonMinor()
            currencyCode = snapshot.currencyCode
            if (amountMinor <= 0) {
                creditsSkipped++
                return@forEach
            }
            if (AttendanceCreditStore.hasCreditForAbsentSession(
                    session.classId,
                    session.sessionDate,
                    attendee.key,
                )
            ) {
                creditsSkipped++
                return@forEach
            }
            AttendanceCreditStore.add(
                glide.model.AttendanceCredit(
                    enrollmentId = enrollment.id,
                    soldPlanId = attendee.soldPlanId,
                    amountMinor = amountMinor,
                    currencyCode = snapshot.currencyCode,
                    description = "Absent: ${attendee.displayName} · $className · ${session.sessionDate}",
                    classId = session.classId,
                    sessionDate = session.sessionDate,
                    attendeeKey = attendee.key,
                ),
            )
            creditsAdded++
            totalCreditMinor += amountMinor
        }

        return AttendanceCreditResult(
            creditsAdded = creditsAdded,
            creditsSkipped = creditsSkipped,
            totalCreditMinor = totalCreditMinor,
            currencyCode = currencyCode,
        )
    }

    fun creditDialogMessage(
        previews: List<AbsentCreditPreview>,
        className: String,
    ): String {
        val eligible = previews.filter { it.eligible && !it.alreadyCredited }
        val ineligible = previews.count { !it.eligible }
        if (eligible.isEmpty()) {
            return buildString {
                append("$className has absent students, but no billing credits can be added.")
                if (ineligible > 0) {
                    append(" Those households may not have an active plan enrollment.")
                }
            }
        }
        val firstEnrollment = SoldPlanEnrollmentStore.forSoldPlan(eligible.first().attendee.soldPlanId)
        val perPerson = eligible.first().amountMinor
        val currency = eligible.first().currencyCode
        val perSessionLabel = formatMoney(perPerson, currency)
        val lessonCount = firstEnrollment?.planSnapshot?.lessonCount
        return buildString {
            append("${eligible.size} absent student")
            if (eligible.size != 1) append("s")
            append(" — credit ")
            append(perSessionLabel)
            append(" per person toward the household's next plan bill?")
            if (lessonCount != null) {
                append(" (Per-person plan price divided across $lessonCount classes.)")
            }
            if (ineligible > 0) {
                append(" ")
                append(ineligible)
                append(" absent student")
                if (ineligible != 1) append("s")
                append(" will not receive a credit (no active enrollment).")
            }
        }
    }
}

private val DEFAULT_CURRENCY_FALLBACK = DEFAULT_CURRENCY_CODE
