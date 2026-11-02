# JWT-only learning path

Use JDK 17 throughout. Begin with [application setup](../../README.md), [scope](../IN-SCOPE-OUT-OF-SCOPE.md), [JWT foundations](JWT-FOUNDATIONS.md), [claim contract](../JWT-CLAIM-CONTRACT.md) and [test matrix](../TEST-MATRIX.md). The application has one unconditional JWT configuration. Public access still passes through security.

## Step walkthroughs

| Step | Purpose | Learning artifact/status |
|---|---|---|
| J1.S1 | Java 17 build/runtime baseline | [Walkthrough](J1.S1-WALKTHROUGH.md) |
| J1.S2 | Unconditional JWT and retirement of other configurations | [Walkthrough](J1.S2-WALKTHROUGH.md) |
| J1.S3 | Credential/signing/bootstrap ownership | [Walkthrough](J1.S3-WALKTHROUGH.md) |
| J2.S1 | Local login and resource authorization | [Walkthrough](J2.S1-WALKTHROUGH.md) |
| J2.S2 | Raw/normalized validation, time and key failures | [Walkthrough](J2.S2-WALKTHROUGH.md) |
| J2.S3 | Diagnostics, lifecycle and identity isolation | [Walkthrough](J2.S3-WALKTHROUGH.md) |
| J3.S1 | Documentation replacement and extraction | [Walkthrough](J3.S1-WALKTHROUGH.md) |
| J3.S2 | Complete source-guide assembly | Planned, no walkthrough yet |
| J3.S3 | Final JDK 17 repository audit | Planned, no walkthrough yet |

J walkthroughs retain actual checkpoint results, including historical suite names and counts. They are not all current source contracts. Current case ownership lives in the test matrix; current wiring lives in project structure/foundations and J1.S3. The original course is recoverable through master/Git history, not required reading on this branch.

The [complete-guide entry point](COMPLETE-GUIDE.md) is a navigation bridge until J3.S2. Follow the [roadmap](../../ROADMAP.md) one numbered step at a time. Do not infer that planned artifacts exist or that the final audit has run.
