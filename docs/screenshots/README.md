# Bug Evidence Screenshots

Screenshots captured during manual exploratory testing of the Claims Management application.

| File | Bug | Description |
|------|-----|-------------|
| `bug-009-year-1111-claim.png` | BUG-009 | Claimant view showing claim with incident date Nov 11, 1111 accepted |
| `bug-009-year-0001-form.png` | BUG-009 | Claim form showing date 01/01/0001 accepted with leading zeros in amount |
| `bug-009-year-0001-review.png` | BUG-009 | Review step showing "January 1, 1" — broken display for year < 1000 |
| `bug-011-dialog-misaligned-rejected.png` | BUG-011 | Confirmation dialog misaligned — "Change status to Rejected?" |
| `bug-011-dialog-misaligned-under-review.png` | BUG-011 | Same misalignment on second status change — "Change status to Under Review?" |
| `bug-012-leading-zeros-amount.png` | BUG-012 | Amount field showing $000000000000000.1 |
| `bug-013-dashboard-stale-zero.png` | BUG-013 | Dashboard showing Total Claims: 0 with 2 claims in the system |
| `claims-list-two-claims.png` | General | My Claims list showing both submitted claims (Jan 1, 1 and Nov 11, 1111) |

## How to save screenshots

On macOS: `Cmd + Shift + 4` → drag to select area → file saves to Desktop
Then move to this folder and rename as above.
