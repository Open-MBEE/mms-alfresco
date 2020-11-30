# Commit Object fix script

Up to 3.2.3, certain commit objects may have duplicated ids for added elements if a view is added from VE. The bug is fixed in 3.2.4. If views have been added from VE, this script can be used to fix the commit objects in elastic directly.

This script can be run anytime after 3.2.4 is installed and before an upgrade to 3.3.0+. MMS does not need to be running.

1. You should have Python 2.7 or higher installed
2. Run `sudo pip install elasticsearch5`
3. Run `python update_commit_object.py`

The update script takes no arguments and assumes it is run on the same machine as elasticsearch. Update line 20 if elasticsearch is not on localhost or port is not 9200.

To run a check to see how many commit objects will be modified without actually updating, comment out line 71.
