# APKeep Dataset Requirement

APKeep expects a parameters file and a network snapshot to initialize a PPM mode,
and expects a rule update file to invoke the verification.

## Parameters

Parameters are expected in a `JSON` file, see [parameters.json](stanford/parameters.json) for example.
The parameters control the working mode of APKeep, and sometimes affect the verification efficiency.
> - `MergeAP` enables the AP Merging process when updating PPM, 
> - `TOTAL_AP_THRESHOLD` and `LOW_MERGEABLE_AP_THRESHOLD` tell APKeep to start an AP Merging process when the number of total AP and the number of mergeable AP exceed these thresholds respectively,
> - `HIGH_MERGEABLE_AP_THRESHOLD` tells APKeep to start an AP Merging process when the number of mergeable AP exceeds this threshold,
> - `GC_INTERVAL` tells APKeep to invoke a JVM garbage collection when the number of processed rules can be divided by the interval,
> - `WRITE_RESULT_INTERVAL` and `PRINT_RESULT_INTERVAL` tell APKeep to write and print the verification statistics when the number of processed rules can be divided by the interval,
> - `FAST_UPDATE_THRESHOLD` tells APKeep to count the number of updates when the verification time is smaller than this millisecond threshold, 
> - `BDD_TABLE_SIZE` tells APKeep the size to allocate the memory for the BDD table.

Default parameters are defined in [Parameters.java](../src/main/java/apkeep/utils/Parameters.java), APKeep will use the default value if it is not specified in the file.
This file is only needed when running APKeep by CLI.

## Snapshot

A network snapshot includes layer one topology, device names, ACL names, vlan-physical port mapping, and nat names.
All the files are optional except for topology.
> - `topo.txt` defines the layer one links, each line represents a directed edge,
> - `devices.txt`defines the devices that should be modeled as a ForwardElement, each line represents a device name,
> - `acls` is a folder that defines the ACLs that should be modeled as an ACLElement, each file represents an ACL name,
> - `vlan.txt` defines the map from a VLAN port to a set of physical ports, each line represents such a map,
> - `nat.txt` defines the nat names that should be modeled as an NATElement, each line represents such a name.

See the example network data set for part of the files, see the parser in [APKeep.java](../src/main/java/apkeep/APKeep.java) for rule format.

## Rule Updates

The `updates` file defines the rule update for APKeep to analyze, each line inserts or removes a rule for the given network.
Currently, APKeep supports analyzing three types of rules:

- `fwd` rule is installed on ForwardElement,
- `acl` rule is installed on ACLElement,
- `nat` rule is installed on NATElement.

The fields of each rule are defined in [apkeep.rules](../src/main/java/apkeep/rules), and the parser is implemented in the method `encodeOneRule(String)` in corresponding [Elements](../src/main/java/apkeep/elements).