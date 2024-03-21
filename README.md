# APKeep

APKeep is a data plane verification tool that checks network invariants for network updates.
The work is published in the [NSDI'20 paper](https://www.usenix.org/conference/nsdi20/presentation/zhang-peng) "APKeep: Realtime Verification for Real Networks".
This branch provides a prototype implementation of APKeep.

## How to run APKeep

APKeep is a `Java` project and can be easily built by `Maven`.
This branch is developed and tested under JDK 11 and Maven v3.9.6. 

### setup dataset

APKeep analyzes the network by taking input from several files in specific formats.
Read [networks](networks/) to know the requirements of the input files.
Make sure you prepare the necessary files before running APKeep.

### run APKeep

To build APKeep, simply run:

```bash
mvn package
```

Then you can invoke APKeep in CLI:

```bash
java -jar target/apkeep-1.0.0.jar
```

APKeep provides several commands to analyze a network.
First, initialize the network snapshot by specifying the folder that contains the required files, for example:

```bash
APKeep>init ../networks/stanford
APKeep>
```

Then, invoke the verification by specifying the file that contains the rule updates.
You can also omit the parameter if the file is in the same folder as the initial snapshot, for example:

```bash
APKeep>update
The stanford dataset
Number of updates: 9052
Total time: 865ms
Update PPM time: 605ms
Check property time: 259ms
Number of APs after insert: 515
Number of APs after update: 2
Number of loops: 20
Average update time: 95.564us
95.0287229341582% < 0.25ms
Memory Usage: 0MB
APKeep>
```

Finally, you can dump loops (if any) or run link failure tests to check the "what if" question, details can be found in the APKeep paper.

```bash
APKeep>dump loops
++++++++++++++++++++++++++++++
loop found for [171.66.255.128/26]:
bbra_rtr,te7/1 bbrb_rtr,te7/1 bbrb_rtr,te6/3 yozb_rtr,te1/1 yozb_rtr,te1/2 yoza_rtr,te1/2 yoza_rtr,te7/1 bbrb_rtr,te7/4 bbrb_rtr,te7/2 cozb_rtr,te2/1 cozb_rtr,te3/1 cozb_rtr_outACL_te3/1_out,inport cozb_rtr_outACL_te3/1_out,permit bbra_rtr,te6/1 bbra_rtr,te7/1
++++++++++++++++++++++++++++++
APKeep>
```

## How to develop using APKeep

The source code of APKeep is in [src/main/java/](src/main/java/), which consists of three modules:
> - `apkeep` is the main module that maintains PPM and verifier;
> - `common` is imported from [AP Transformer](https://www.cs.utexas.edu/users/lam/NRL/), which wraps BDD operation on network packets;
> - `JDD` is imported as a Maven dependency, which is an [open-source](https://bitbucket.org/vahidi/jdd/) BDD library for Java.

To develop your own data plane verifier using APKeep, you might use or modify part of the source files.

### package core

- **APKeeper** manages PPM, including the data structures of `port_aps` and `ap_ports`, as well as the algorithms updating PPM, such as `Split`, `Transfer`, and `Merge`.
- **Network** manages `Element`s for network devices, also provides APIs to interact with input files.
- **ChangeItem** defines the behavior change in the form of 3-tuple.

### package element

- **Element** manages `aps` for each `port`, including the algorithm `EncodingRules`, `IdentifyChanges`, and `UpdatingPredicates`.
- **ForwardElement** inherits Element and optimizes updating algorithms for IP forwarding rule using prefix trie tree.
- **ACLElement** inherits Element and works on a prioritized acl rule list.
- **NATElement** inherits Element and overwrites algorithms for updating `rewrite table`.

### package checker
- **Loop** defines the forwarding loop and records the relevant packets.
- **ForwardingGraph** defines the forwarding graph for a set of `AP`s, including the nodes and ports holding such `ap`.
- **Checker** implements the algorithms to check invariants, including `ConstructForwardingGraph`, `TraverseForwardingGraph`, and directly `TraversePPM` without constructing a forwarding graph.

### the others

The other packages define some useful data structures during verification, please check the code for details.

## For Researchers

To evaluate APKeep using the experiments from the NSDI paper, we provide [ExampleExp.java](src/main/java/apkeep/main/main.java).
You can find part of the datasets in [networks](networks/).

## Support

Feel free to contact us if issues occur to you.

- Peng Zhang (p-zhang@xjtu.edu.cn)
- Xu Liu (x.liu.reason@outlook.com)

## License
APKeep is released under [license](LICENSE).
