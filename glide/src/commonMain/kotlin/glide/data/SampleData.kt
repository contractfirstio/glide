package glide.data

import glide.model.AcademicTerm
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
import glide.model.majorToMinor

/**
 * Populates in-memory stores with realistic demo data for manual testing.
 * Runs once per app launch when stores are empty.
 */
object SampleData {
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
            notes = "Friend on Emma and James packs — tests shared membership",
            createdAtMillis = now - 10 * day,
        )
        listOf(leo, mia, noah, ivy, sam, zoe, alex).forEach { RelatedPersonStore.create(it) }

        // Customer 1: outstanding issued bill — test Record payment
        val emmaGroup = PeopleGroup(
            id = "sample-customer-emma",
            type = PeopleGroupType.CUSTOMER,
            mainContactId = emma.id,
            relatedPersonIds = listOf(mia.id, noah.id, alex.id),
            status = PeopleGroupStatus.Won,
            planId = rollingPack.id,
            createdAtMillis = now - 10 * day,
        )
        seedCustomerBilling(emmaGroup, rollingPack) { enrollment ->
            BillStore.issueInitialPackBill(enrollment, exportInvoice = false)
        }

        // Customer 2: fully paid — test paid bill display
        val jamesGroup = PeopleGroup(
            id = "sample-customer-james",
            type = PeopleGroupType.CUSTOMER,
            mainContactId = james.id,
            relatedPersonIds = listOf(ivy.id, sam.id, alex.id),
            planId = rollingPack.id,
            mainContactAttendsClass = false,
            status = PeopleGroupStatus.Won,
            createdAtMillis = now - 8 * day,
        )
        seedCustomerBilling(jamesGroup, rollingPack) { enrollment ->
            val bill = BillStore.issueInitialPackBill(enrollment, exportInvoice = false)
            PaymentStore.recordFullPayment(
                bill = bill,
                method = PaymentMethod.BANK_TRANSFER,
                reference = "BACS-88421",
                receivedAtMillis = now - 7 * day,
            )
        }

        // Customer 3: paid initial + outstanding renewal — test Issue bill / outstanding
        val sarahGroup = PeopleGroup(
            id = "sample-customer-sarah",
            type = PeopleGroupType.CUSTOMER,
            mainContactId = sarah.id,
            relatedPersonIds = listOf(leo.id, zoe.id),
            planId = rollingPack.id,
            status = PeopleGroupStatus.Won,
            notes = "Family pack",
            createdAtMillis = now - 6 * day,
        )
        seedCustomerBilling(sarahGroup, rollingPack) { enrollment ->
            val first = BillStore.issueInitialPackBill(enrollment, exportInvoice = false)
            PaymentStore.recordFullPayment(
                bill = first,
                method = PaymentMethod.CARD,
                reference = "CARD-22901",
                receivedAtMillis = now - 5 * day,
            )
            BillStore.issuePackBill(enrollment, exportInvoice = false)
        }

        // Leads for non-billing UI smoke tests
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
                status = PeopleGroupStatus.Proposal,
                planId = rollingPack.id,
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
                createdAtMillis = now - day,
            ),
        )

        seedScheduling(now, day)
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

        ScheduledClassStore.create(
            ScheduledClass(
                id = "sample-class-tuesday-ballet",
                name = "Tuesday Beginner Ballet",
                termIds = listOf(springTerm.id, summerTerm.id),
                customerGroupIds = listOf("sample-customer-emma", "sample-customer-james"),
                locationId = studioA.id,
                dayOfWeek = DayOfWeek.TUESDAY,
                startTime = "16:00",
                endTime = "17:00",
                notes = "Runs across Spring and Summer",
                createdAtMillis = now - 4 * day,
            ),
        )
        ScheduledClassStore.create(
            ScheduledClass(
                id = "sample-class-saturday-drama",
                name = "Saturday Drama Club",
                termIds = listOf(springTerm.id),
                customerGroupIds = listOf("sample-customer-sarah"),
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
                notes = "One-off open day — single date only",
                createdAtMillis = now - 4 * day,
            ),
        )
    }

    private fun seedCustomerBilling(
        group: PeopleGroup,
        plan: Plan,
        configureBills: (glide.model.PackEnrollment) -> Unit,
    ) {
        PeopleGroupStore.seedCustomer(group)
        val enrollment = PackEnrollmentStore.createForCustomerGroup(
            group = group,
            planSnapshot = PlanSnapshot.from(plan),
        ) ?: return
        configureBills(enrollment)
    }
}
