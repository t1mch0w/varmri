"""A Node for running the Cloudlab command line tools, to create,
monitor, and terminate experiments.

Instructions:
Please see the [README](https://gitlab.flux.utah.edu/stoller/portal-tools/blob/master/README.md) file in git repository.
"""

# Import the Portal object.
import geni.portal as portal
import geni.rspec.pg as rspec

# Create a Request object to start building the RSpec.
request = portal.context.makeRequestRSpec()
 
# Add a XenVM (named "node") to the request
node = request.RawPC("node")

# Write the request in RSpec format
portal.context.printRequestRSpec()
