package glide.data

import glide.model.AcademicTerm
import glide.model.AttendanceStatus
import glide.model.ClassAttendanceRecord
import glide.model.ClassSessionKey
import glide.model.ClassLocation
import glide.model.Contact
import glide.model.DayOfWeek
import glide.model.PaymentMethod
import glide.model.PeopleGroup
import glide.model.PeopleGroupStatus
import glide.model.PeopleGroupType
import glide.model.Plan
import glide.model.PlanKind
import glide.model.PlanSnapshot
import glide.model.RelatedPerson
import glide.model.ScheduledClass
import glide.model.dateRange
import glide.model.majorToMinor
import glide.model.occursOn
import java.time.LocalDate

/**
 * Populates in-memory stores with realistic demo data for manual testing.
 * Runs once per app launch when stores are empty.
 */
object SampleData {
    /** Fixed "today" for stable demo data (summer term 2026). */
    private val sampleToday = LocalDate.of(2026, 5, 23)

    private val deferredBillingActions = mutableListOf<() -> Unit>()

    fun loadIfEmpty() {
        if (PlanStore.plans.isNotEmpty()) return

        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000

        val rollingPack = Plan(
            id = "sample-plan-rolling-10",
            kind = PlanKind.MULTI_LESSON_PACK,
            name = "10 Class Rolling Pack",
            lessonCount = 10,
            rolling = true,
            priceAmountMinor = majorToMinor(120.0),
            notes = "Standard term pack",
            createdAtMillis = now - 30 * day,
        )
        val singleLesson = Plan(
            id = "sample-plan-single",
            kind = PlanKind.SINGLE_LESSON_PACK,
            name = "Single Trial Lesson",
            lessonCount = 1,
            rolling = false,
            priceAmountMinor = majorToMinor(15.0),
            createdAtMillis = now - 30 * day,
        )
        PlanStore.create(rollingPack)
        PlanStore.create(singleLesson)

        val emma = Contact(
            id = "sample-contact-emma",
            name = "Emma Walsh",
            dateOfBirth = "12/03/1988",
            email = "emma.walsh@example.com",
            phone = "07700 900 101",
            createdAtMillis = now - 20 * day,
        )
        val james = Contact(
            id = "sample-contact-james",
            name = "James Chen",
            dateOfBirth = "05/07/1992",
            email = "james.chen@example.com",
            phone = "07700 900 202",
            createdAtMillis = now - 18 * day,
        )
        val sarah = Contact(
            id = "sample-contact-sarah",
            name = "Sarah Thompson",
            email = "sarah.thompson@example.com",
            phone = "07700 900 303",
            createdAtMillis = now - 14 * day,
        )
        ContactStore.seed(emma)
        ContactStore.seed(james)
        ContactStore.seed(sarah)

        val leo = RelatedPerson(
            id = "sample-related-leo",
            name = "Leo Thompson",
            dateOfBirth = "08/11/2014",
            notes = "Sarah's son",
            createdAtMillis = now - 14 * day,
        )
        val mia = RelatedPerson(
            id = "sample-related-mia",
            name = "Mia Walsh",
            dateOfBirth = "22/01/2012",
            notes = "Emma's daughter",
            createdAtMillis = now - 12 * day,
        )
        val noah = RelatedPerson(
            id = "sample-related-noah",
            name = "Noah Walsh",
            dateOfBirth = "14/06/2015",
            notes = "Emma's son",
            createdAtMillis = now - 12 * day,
        )
        val ivy = RelatedPerson(
            id = "sample-related-ivy",
            name = "Ivy Chen",
            dateOfBirth = "03/09/1990",
            notes = "James's partner",
            createdAtMillis = now - 11 * day,
        )
        val sam = RelatedPerson(
            id = "sample-related-sam",
            name = "Sam Chen",
            dateOfBirth = "19/04/2018",
            notes = "James's son",
            createdAtMillis = now - 11 * day,
        )
        val zoe = RelatedPerson(
            id = "sample-related-zoe",
            name = "Zoe Thompson",
            dateOfBirth = "30/07/2016",
            notes = "Sarah's daughter",
            createdAtMillis = now - 13 * day,
        )
        val alex = RelatedPerson(
            id = "sample-related-alex",
            name = "Alex Morgan",
            dateOfBirth = "11/02/2010",
            notes = "Related on Emma and James packs — different classes",
            createdAtMillis = now - 10 * day,
        )
        listOf(leo, mia, noah, ivy, sam, zoe, alex).forEach { RelatedPersonStore.create(it) }

        seedScheduling(now, day)

        // Customer 1: scheduled bill — test issue toggle and invoice generation
        val emmaGroup = PeopleGroup(
            id = SAMPLE_CUSTOMER_EMMA,
            type = PeopleGroupType.CUSTOMER,
            mainContactId = emma.id,
            relatedPersonIds = listOf(mia.id, noah.id, alex.id),
            status = PeopleGroupStatus.Contacted,
            planId = rollingPack.id,
            createdAtMillis = now - 10 * day,
        )
        registerCustomerBilling(emmaGroup, rollingPack) { enrollment ->
            BillStore.createInitialPackBill(enrollment)
        }

        // Customer 2: fully paid — test paid bill display
        val jamesGroup = PeopleGroup(
            id = SAMPLE_CUSTOMER_JAMES,
            type = PeopleGroupType.CUSTOMER,
            mainContactId = james.id,
            relatedPersonIds = listOf(ivy.id, sam.id, alex.id),
            planId = rollingPack.id,
            mainContactAttendsClass = false,
            status = PeopleGroupStatus.Contacted,
            createdAtMillis = now - 8 * day,
        )
        registerCustomerBilling(jamesGroup, rollingPack) { enrollment ->
            val bill = BillStore.createInitialPackBill(enrollment)
            BillStore.setIssued(bill.id, issued = true, issuedAtMillis = now - 8 * day)
            PaymentStore.recordFullPayment(
                bill = BillStore.findById(bill.id)!!,
                method = PaymentMethod.BANK_TRANSFER,
                reference = "BACS-88421",
                receivedAtMillis = now - 7 * day,
            )
        }

        // Customer 3: paid initial + outstanding renewal — test Issue bill / outstanding
        val sarahGroup = PeopleGroup(
            id = SAMPLE_CUSTOMER_SARAH,
            type = PeopleGroupType.CUSTOMER,
            mainContactId = sarah.id,
            relatedPersonIds = listOf(leo.id, zoe.id),
            planId = rollingPack.id,
            status = PeopleGroupStatus.Contacted,
            notes = "Family pack",
            createdAtMillis = now - 6 * day,
        )
        registerCustomerBilling(sarahGroup, rollingPack) { enrollment ->
            val first = BillStore.createInitialPackBill(enrollment)
            BillStore.setIssued(first.id, issued = true, issuedAtMillis = now - 6 * day)
            PaymentStore.recordFullPayment(
                bill = BillStore.findById(first.id)!!,
                method = PaymentMethod.CARD,
                reference = "CARD-22901",
                receivedAtMillis = now - 5 * day,
            )
            BillStore.createRenewalBill(enrollment)
        }

        assignPackSchedulesForRollingGroups()
        seedPastAttendance(now, sampleToday)
        deferredBillingActions.forEach { it() }
        RollingPackBillingService.syncAllActiveRollingPackBilling()

        // Issued renewal overdue for payment alert testing
        BillStore.forPeopleGroup(SAMPLE_CUSTOMER_SARAH)
            .firstOrNull { it.status == glide.model.BillStatus.SCHEDULED && it.isRenewalBill() }
            ?.let { renewal ->
                BillStore.setIssued(renewal.id, issued = true, issuedAtMillis = now - 10 * day)
            }

        val ella = RelatedPerson(
            id = "sample-related-ella",
            name = "Ella O'Brien",
            dateOfBirth = "25/03/2013",
            notes = "On Mike's lead only — not on a customer pack yet",
            createdAtMillis = now - 2 * day,
        )
        RelatedPersonStore.create(ella)

        PeopleGroupStore.create(
            PeopleGroup(
                id = "sample-lead-mike",
                type = PeopleGroupType.LEAD,
                contactName = "Mike O'Brien",
                email = "mike.obrien@example.com",
                phone = "07700 900 404",
                relatedPersonIds = listOf(ella.id),
                status = PeopleGroupStatus.WaitingReply,
                planId = rollingPack.id,
                planStartDate = sampleToday.toString(),
                notes = "Interested in evening classes",
                createdAtMillis = now - 2 * day,
            ),
        )
        PeopleGroupStore.create(
            PeopleGroup(
                id = "sample-lead-trial",
                type = PeopleGroupType.LEAD,
                contactName = "Priya Nair",
                status = PeopleGroupStatus.Contacted,
                planId = singleLesson.id,
                planStartDate = sampleToday.toString(),
                createdAtMillis = now - day,
            ),
        )
    }

    private fun seedScheduling(now: Long, day: Long) {
        if (TermStore.terms.isNotEmpty()) return

        val springTerm = AcademicTerm(
            id = "sample-term-spring-2026",
            name = "Spring 2026",
            startDate = "2026-01-05",
            endDate = "2026-04-03",
            notes = "Includes half-term break in February.",
            createdAtMillis = now - 5 * day,
        )
        TermStore.create(springTerm)

        val summerTerm = AcademicTerm(
            id = "sample-term-summer-2026",
            name = "Summer 2026",
            startDate = "2026-04-20",
            endDate = "2026-07-17",
            createdAtMillis = now - 5 * day,
        )
        TermStore.create(summerTerm)

        val studioA = ClassLocation(
            id = "sample-location-studio-a",
            name = "Studio A",
            maxCapacity = 12,
            notes = "Ground floor · mirrors and barre",
            createdAtMillis = now - 5 * day,
        )
        LocationStore.create(studioA)

        val mainHall = ClassLocation(
            id = "sample-location-main-hall",
            name = "Main Hall",
            maxCapacity = 24,
            createdAtMillis = now - 5 * day,
        )
        LocationStore.create(mainHall)

        // One customer group per class
        ScheduledClassStore.create(
            ScheduledClass(
                id = "sample-class-tuesday-ballet",
                name = "Tuesday Beginner Ballet",
                termIds = listOf(springTerm.id, summerTerm.id),
                customerGroupIds = listOf(SAMPLE_CUSTOMER_EMMA),
                locationId = studioA.id,
                dayOfWeek = DayOfWeek.TUESDAY,
                startTime = "16:00",
                endTime = "17:00",
                notes = "Emma's household — rolling pack",
                createdAtMillis = now - 4 * day,
            ),
        )
        ScheduledClassStore.create(
            ScheduledClass(
                id = "sample-class-thursday-tap",
                name = "Thursday Tap",
                termIds = listOf(springTerm.id, summerTerm.id),
                customerGroupIds = listOf(SAMPLE_CUSTOMER_JAMES),
                locationId = studioA.id,
                dayOfWeek = DayOfWeek.THURSDAY,
                startTime = "17:30",
                endTime = "18:30",
                notes = "James household — main contact does not attend",
                createdAtMillis = now - 4 * day,
            ),
        )
        ScheduledClassStore.create(
            ScheduledClass(
                id = "sample-class-saturday-drama",
                name = "Saturday Drama Club",
                termIds = listOf(springTerm.id, summerTerm.id),
                customerGroupIds = listOf(SAMPLE_CUSTOMER_SARAH),
                locationId = mainHall.id,
                dayOfWeek = DayOfWeek.SATURDAY,
                startTime = "10:30",
                endTime = "12:00",
                createdAtMillis = now - 4 * day,
            ),
        )
        ScheduledClassStore.create(
            ScheduledClass(
                id = "sample-class-open-day",
                name = "Spring Open Day Workshop",
                termIds = listOf(springTerm.id),
                locationId = studioA.id,
                dayOfWeek = DayOfWeek.WEDNESDAY,
                singleDate = "2026-03-18",
                startTime = "14:00",
                endTime = "16:00",
                notes = "One-off open day — no enrolled groups",
                createdAtMillis = now - 4 * day,
            ),
        )
    }

    private fun registerCustomerBilling(
        group: PeopleGroup,
        plan: Plan,
        configureBills: (glide.model.PackEnrollment) -> Unit,
    ) {
        PeopleGroupStore.seedCustomer(group)
        val enrollment = PackEnrollmentStore.createForCustomerGroup(
            group = group,
            planSnapshot = PlanSnapshot.from(plan),
        ) ?: return
        deferredBillingActions.add { configureBills(enrollment) }
    }

    private fun assignPackSchedulesForRollingGroups() {
        ScheduledClassStore.classes.forEach { scheduledClass ->
            scheduledClass.customerGroupIds.forEach { groupId ->
                val rolling = PackEnrollmentStore.forPeopleGroup(groupId)?.planSnapshot?.rolling == true
                if (rolling) {
                    assignPackClassSchedule(groupId, scheduledClass)
                }
            }
        }
    }

    private fun seedPastAttendance(recordedAtMillis: Long, asOf: LocalDate) {
        for (scheduledClass in ScheduledClassStore.classes) {
            if (scheduledClass.customerGroupIds.isEmpty()) continue
            val sessionDates = mutableSetOf<LocalDate>()
            for (termId in scheduledClass.termIds) {
                val term = TermStore.findById(termId) ?: continue
                val range = term.dateRange() ?: continue
                var date = range.start
                while (!date.isAfter(range.endInclusive) && date.isBefore(asOf)) {
                    if (scheduledClass.occursOn(date)) {
                        sessionDates.add(date)
                    }
                    date = date.plusDays(1)
                }
            }
            for (date in sessionDates) {
                val attendees = attendeesForClass(scheduledClass, date)
                if (attendees.isEmpty()) continue
                val session = ClassSessionKey(
                    scheduledClassId = scheduledClass.id,
                    sessionDate = date.toString(),
                )
                for (attendee in attendees) {
                    ClassAttendanceStore.seed(
                        ClassAttendanceRecord(
                            scheduledClassId = session.scheduledClassId,
                            sessionDate = session.sessionDate,
                            attendeeKey = attendee.key,
                            status = AttendanceStatus.PRESENT,
                            recordedAtMillis = recordedAtMillis,
                        ),
                    )
                }
                ClassAttendanceStore.markSessionSubmitted(session)
            }
        }
    }
}

private const val SAMPLE_CUSTOMER_EMMA = "sample-customer-emma"
private const val SAMPLE_CUSTOMER_JAMES = "sample-customer-james"
private const val SAMPLE_CUSTOMER_SARAH = "sample-customer-sarah"
