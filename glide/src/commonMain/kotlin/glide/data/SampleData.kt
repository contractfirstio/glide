package glide.data

import glide.model.AcademicTerm
import glide.model.AttendanceStatus
import glide.model.ClassAttendanceRecord
import glide.model.ClassSessionKey
import glide.model.ClassLocation
import glide.model.Client
import glide.model.DayOfWeek
import glide.model.PaymentMethod
import glide.model.PeopleGroup
import glide.model.PeopleGroupStatus
import glide.model.PeopleGroupType
import glide.model.Plan
import glide.model.PlanKind
import glide.model.PlanSnapshot
import glide.model.Student
import glide.model.ScheduledClass
import glide.model.dateRange
import glide.model.majorToMinor
import glide.model.occursOn
import glide.model.sessionHasEndedForAttendance
import java.time.LocalDate
import java.time.LocalTime

/**
 * Populates in-memory stores with realistic demo data for manual testing.
 * Runs once per app launch when stores are empty.
 */
object SampleData {
    /** Fixed "today" for stable demo data (summer term 2026). */
    private val sampleToday = LocalDate.of(2026, 5, 23)

    /** Sold-plan start for sample customers — summer term start so past class sessions count. */
    private const val SAMPLE_PLAN_START_DATE = "2026-04-20"

    private val deferredBillingActions = mutableListOf<() -> Unit>()

    fun loadIfEmpty() {
        if (PlanStore.plans.isNotEmpty()) return

        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000

        val rollingPlan = Plan(
            id = "sample-plan-rolling-10",
            kind = PlanKind.MULTI_LESSON_PLAN,
            name = "10 Class Rolling Plan",
            lessonCount = 10,
            rolling = true,
            priceAmountMinor = majorToMinor(120.0),
            notes = "Standard term plan",
            createdAtMillis = now - 30 * day,
        )
        val singleLesson = Plan(
            id = "sample-plan-single",
            kind = PlanKind.SINGLE_LESSON_PLAN,
            name = "Single Trial Lesson",
            lessonCount = 1,
            rolling = false,
            priceAmountMinor = majorToMinor(15.0),
            createdAtMillis = now - 30 * day,
        )
        PlanStore.create(rollingPlan)
        PlanStore.create(singleLesson)

        val emma = Client(
            id = "sample-client-emma",
            name = "Emma Walsh",
            dateOfBirth = "12/03/1988",
            email = "emma.walsh@example.com",
            phone = "07700 900 101",
            createdAtMillis = now - 20 * day,
        )
        val james = Client(
            id = "sample-client-james",
            name = "James Chen",
            dateOfBirth = "05/07/1992",
            email = "james.chen@example.com",
            phone = "07700 900 202",
            createdAtMillis = now - 18 * day,
        )
        val sarah = Client(
            id = "sample-client-sarah",
            name = "Sarah Thompson",
            email = "sarah.thompson@example.com",
            phone = "07700 900 303",
            createdAtMillis = now - 14 * day,
        )
        ClientStore.seed(emma)
        ClientStore.seed(james)
        ClientStore.seed(sarah)

        val leo = Student(
            id = "sample-student-leo",
            name = "Leo Thompson",
            dateOfBirth = "08/11/2014",
            notes = "Sarah's son",
            createdAtMillis = now - 14 * day,
        )
        val mia = Student(
            id = "sample-student-mia",
            name = "Mia Walsh",
            dateOfBirth = "22/01/2012",
            notes = "Emma's daughter",
            createdAtMillis = now - 12 * day,
        )
        val noah = Student(
            id = "sample-student-noah",
            name = "Noah Walsh",
            dateOfBirth = "14/06/2015",
            notes = "Emma's son",
            createdAtMillis = now - 12 * day,
        )
        val ivy = Student(
            id = "sample-student-ivy",
            name = "Ivy Chen",
            dateOfBirth = "03/09/1990",
            notes = "James's partner",
            createdAtMillis = now - 11 * day,
        )
        val sam = Student(
            id = "sample-student-sam",
            name = "Sam Chen",
            dateOfBirth = "19/04/2018",
            notes = "James's son",
            createdAtMillis = now - 11 * day,
        )
        val zoe = Student(
            id = "sample-student-zoe",
            name = "Zoe Thompson",
            dateOfBirth = "30/07/2016",
            notes = "Sarah's daughter",
            createdAtMillis = now - 13 * day,
        )
        val alex = Student(
            id = "sample-student-alex",
            name = "Alex Morgan",
            dateOfBirth = "11/02/2010",
            notes = "Student on Emma and James plans — different classes",
            createdAtMillis = now - 10 * day,
        )
        listOf(leo, mia, noah, ivy, sam, zoe, alex).forEach { StudentStore.create(it) }

        seedScheduling(now, day)

        // Customer 1: scheduled bill — test issue toggle and invoice generation
        val emmaGroup = PeopleGroup(
            id = SAMPLE_CUSTOMER_EMMA,
            type = PeopleGroupType.CUSTOMER,
            mainClientId = emma.id,
            studentIds = listOf(mia.id, noah.id, alex.id),
            status = PeopleGroupStatus.Contacted,
            planId = rollingPlan.id,
            planStartDate = SAMPLE_PLAN_START_DATE,
            createdAtMillis = now - 10 * day,
        )
        registerCustomerBilling(emmaGroup, rollingPlan) { enrollment ->
            BillStore.createInitialPlanBill(enrollment)
        }

        // Customer 2: fully paid — test paid bill display
        val jamesGroup = PeopleGroup(
            id = SAMPLE_CUSTOMER_JAMES,
            type = PeopleGroupType.CUSTOMER,
            mainClientId = james.id,
            studentIds = listOf(ivy.id, sam.id, alex.id),
            planId = rollingPlan.id,
            mainClientAttendsClass = false,
            status = PeopleGroupStatus.Contacted,
            planStartDate = SAMPLE_PLAN_START_DATE,
            createdAtMillis = now - 8 * day,
        )
        registerCustomerBilling(jamesGroup, rollingPlan) { enrollment ->
            val bill = BillStore.createInitialPlanBill(enrollment)
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
            mainClientId = sarah.id,
            studentIds = listOf(leo.id, zoe.id),
            planId = rollingPlan.id,
            planStartDate = SAMPLE_PLAN_START_DATE,
            status = PeopleGroupStatus.Contacted,
            notes = "Family plan",
            createdAtMillis = now - 6 * day,
        )
        registerCustomerBilling(sarahGroup, rollingPlan) { enrollment ->
            val first = BillStore.createInitialPlanBill(enrollment)
            BillStore.setIssued(first.id, issued = true, issuedAtMillis = now - 6 * day)
            PaymentStore.recordFullPayment(
                bill = BillStore.findById(first.id)!!,
                method = PaymentMethod.CARD,
                reference = "CARD-22901",
                receivedAtMillis = now - 5 * day,
            )
            BillStore.createRenewalBill(enrollment)
        }

        assignPlanSchedulesForRollingGroups()
        seedPastAttendance(now)
        deferredBillingActions.forEach { it() }
        RollingPlanBillingService.syncAllActiveRollingPlanBilling()

        // Issued renewal overdue for payment alert testing
        BillStore.forPeopleGroup(SAMPLE_CUSTOMER_SARAH)
            .firstOrNull { it.status == glide.model.BillStatus.SCHEDULED && it.isRenewalBill() }
            ?.let { renewal ->
                BillStore.setIssued(renewal.id, issued = true, issuedAtMillis = now - 10 * day)
            }

        val ella = Student(
            id = "sample-student-ella",
            name = "Ella O'Brien",
            dateOfBirth = "25/03/2013",
            notes = "On Mike's lead only — not on a customer plan yet",
            createdAtMillis = now - 2 * day,
        )
        StudentStore.create(ella)

        PeopleGroupStore.create(
            PeopleGroup(
                id = "sample-lead-mike",
                type = PeopleGroupType.LEAD,
                clientName = "Mike O'Brien",
                email = "mike.obrien@example.com",
                phone = "07700 900 404",
                studentIds = listOf(ella.id),
                status = PeopleGroupStatus.WaitingReply,
                planId = rollingPlan.id,
                planStartDate = sampleToday.toString(),
                notes = "Interested in evening classes",
                createdAtMillis = now - 2 * day,
            ),
        )
        PeopleGroupStore.create(
            PeopleGroup(
                id = "sample-lead-trial",
                type = PeopleGroupType.LEAD,
                clientName = "Priya Nair",
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
            addressLine1 = "12 Dance Lane",
            city = "Central",
            notes = "Ground floor · mirrors and barre",
            createdAtMillis = now - 5 * day,
        )
        LocationStore.create(studioA)

        val mainHall = ClassLocation(
            id = "sample-location-main-hall",
            name = "Main Hall",
            maxCapacity = 24,
            addressLine1 = "48 Community Road",
            city = "Wan Chai",
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
                notes = "Emma's household — rolling plan",
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
                notes = "James household — main client does not attend",
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
        configureBills: (glide.model.PlanEnrollment) -> Unit,
    ) {
        PeopleGroupStore.seedCustomer(group)
        val enrollment = PlanEnrollmentStore.createForCustomerGroup(
            group = group,
            planSnapshot = PlanSnapshot.from(plan),
        ) ?: return
        deferredBillingActions.add { configureBills(enrollment) }
    }

    private fun assignPlanSchedulesForRollingGroups() {
        ScheduledClassStore.classes.forEach { scheduledClass ->
            scheduledClass.customerGroupIds.forEach { groupId ->
                val rolling = PlanEnrollmentStore.forPeopleGroup(groupId)?.planSnapshot?.rolling == true
                if (rolling) {
                    assignPlanClassSchedule(groupId, scheduledClass)
                }
            }
        }
    }

    private fun seedPastAttendance(recordedAtMillis: Long) {
        val today = LocalDate.now()
        val now = LocalTime.now()
        val outstandingSession = latestCompletedSessionWithAttendees(today, now) ?: return

        for (scheduledClass in ScheduledClassStore.classes) {
            if (scheduledClass.customerGroupIds.isEmpty()) continue
            val sessionDates = mutableSetOf<LocalDate>()
            for (termId in scheduledClass.termIds) {
                val term = TermStore.findById(termId) ?: continue
                val range = term.dateRange() ?: continue
                var date = range.start
                while (!date.isAfter(range.endInclusive) && !date.isAfter(today)) {
                    if (
                        scheduledClass.occursOn(date) &&
                        scheduledClass.sessionHasEndedForAttendance(sessionDate = date, today = today, now = now)
                    ) {
                        sessionDates.add(date)
                    }
                    date = date.plusDays(1)
                }
            }
            for (date in sessionDates) {
                val session = ClassSessionKey(
                    scheduledClassId = scheduledClass.id,
                    sessionDate = date.toString(),
                )
                if (session == outstandingSession) continue
                val attendees = attendeesForClass(scheduledClass, date)
                if (attendees.isEmpty()) continue
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

    private fun latestCompletedSessionWithAttendees(
        today: LocalDate,
        now: LocalTime,
    ): ClassSessionKey? {
        var latest: ClassSessionKey? = null
        var latestDate: LocalDate? = null
        for (scheduledClass in ScheduledClassStore.classes) {
            if (scheduledClass.customerGroupIds.isEmpty()) continue
            for (termId in scheduledClass.termIds) {
                val term = TermStore.findById(termId) ?: continue
                val range = term.dateRange() ?: continue
                var date = range.start
                while (!date.isAfter(range.endInclusive) && !date.isAfter(today)) {
                    if (
                        scheduledClass.occursOn(date) &&
                        scheduledClass.sessionHasEndedForAttendance(sessionDate = date, today = today, now = now) &&
                        attendeesForClass(scheduledClass, date).isNotEmpty()
                    ) {
                        if (latestDate == null || date.isAfter(latestDate)) {
                            latestDate = date
                            latest = ClassSessionKey(
                                scheduledClassId = scheduledClass.id,
                                sessionDate = date.toString(),
                            )
                        }
                    }
                    date = date.plusDays(1)
                }
            }
        }
        return latest
    }
}

private const val SAMPLE_CUSTOMER_EMMA = "sample-customer-emma"
private const val SAMPLE_CUSTOMER_JAMES = "sample-customer-james"
private const val SAMPLE_CUSTOMER_SARAH = "sample-customer-sarah"
