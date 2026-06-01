# Local Smoke Test

## Start App

Run:

```bash
./mvnw spring-boot:run
```

Open:

- `http://localhost:8080`

Open the browser DevTools console before logging in.

## Customer Flow

1. Register or log in as a customer.
2. Refresh the page and confirm the session restores.
3. Open the menu and confirm food items load.
4. Create an order.
5. Open Orders and confirm your order appears.
6. Edit your own order.
7. Delete or cancel your own order if needed.
8. Create another order for payment testing.
9. Open Payments.
10. Confirm the order dropdown appears.
11. Select your order.
12. Confirm the amount auto-fills from the order total.
13. Create the payment.
14. Confirm the payment appears with status `DUE`.
15. Edit only safe fields such as payment method and due date.
16. Confirm you cannot mark the payment `PAID` manually.
17. Log out.

## Chef Flow

1. Log in as a chef.
2. Add a food item.
3. Update the same food item.
4. Delete the same food item.
5. Open Orders and confirm same-society orders are visible.
6. Accept a customer order.
7. Deliver the accepted order.
8. Open Complaints and confirm same-society complaints are visible.
9. Update complaint operational fields such as status and assigned-to.
10. Open Payments and confirm payment records are visible.
11. Confirm the payment form is hidden and create/edit/delete actions are not available.

## Complaint Flow

1. Log in as a customer.
2. Create a complaint.
3. Confirm only your own complaints are visible.
4. Edit your own complaint title or description.
5. Delete your own complaint if needed.
6. Log in as a chef.
7. Confirm the chef can view same-society complaints.
8. Update complaint status or assigned-to as the chef.
9. Confirm complaint delete is not available to the chef.

## Security Checks

1. Log out and confirm the dashboard does not restore on refresh.
2. Refresh after login and confirm the session restores.
3. Clear the token manually or use an invalid token and confirm the app returns to login.
4. Confirm no Spring Security default login page appears.
5. Confirm unauthenticated API access returns JSON `401`.

## Console Checks

Verify:

- no red JavaScript errors
- no failed static asset requests
- no unexpected `401` responses after login
