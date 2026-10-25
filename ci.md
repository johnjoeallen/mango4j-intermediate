# CI

Continuous integration jobs are common accross all mango repositories.
The common CI lives here: https://scm.mastercard.int/stash/projects/MANGO/repos/ci/browse

## Automatic dependency updator

We have a weekly running job, that makes use of `update-properties` goal, to attempt and upgrade any dependencies possible.
Examples on what restrictions we can put are found here: https://www.mojohaus.org/versions/versions-maven-plugin/examples/update-properties.html
Main restriction to using this, is that the versions MUST be defined as properties.
The dependency updator and how it works will be fully documented in another place and link to that will be added here.