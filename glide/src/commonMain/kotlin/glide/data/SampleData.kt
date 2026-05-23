package glide.data

import glide.model.Contact
import glide.model.PaymentMethod
import glide.model.PeopleGroup
import glide.model.PeopleGroupStatus
import glide.model.PeopleGroupType
import glide.model.Plan
import glide.model.PlanKind
import glide.model.PlanSnapshot
import glide.model.RelatedPerson
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
        RelatedPersonStore.create(leo)

        // Customer 1: outstanding issued bill — test Record payment
        val emmaGroup = PeopleGroup(
            id = "sample-customer-emma",
            type = PeopleGroupType.CUSTOMER,
            mainContactId = emma.id,
            relatedPersonIds = emptyList(),
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
            planId = rollingPack.id,
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
            relatedPersonIds = listOf(leo.id),
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
        PeopleGroupStore.create(
            PeopleGroup(
                id = "sample-lead-mike",
                type = PeopleGroupType.LEAD,
                contactName = "Mike O'Brien",
                email = "mike.obrien@example.com",
                phone = "07700 900 404",
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
