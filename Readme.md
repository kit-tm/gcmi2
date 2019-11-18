
# Performance Improvements and Filtering for SDN Control Message Interception
This is the repository of my master thesis "Performance Improvements and Filtering for SDN Control Message Interception". In this thesis, an enhanced framework for Generic Control Message Interception (GCMI) was developed.

## Overview
This repository consists of
 - The implementation of the enhanced framework in Java
 - PyBench, a benchmarking tool written in Python
 - A modified version of the [Floodlight SDN controller](https://github.com/floodlight/floodlight) that saves timestamps when sending and receiving messages
 - Scripts to reproduce different benchmarking experiments described in my thesis
 
## What is GCMI?
Generic Control Message Interception (GCMI) means the interception of [OpenFlow](https://www.opennetworking.org/software-defined-standards/specifications/) messages exchanged between switches and controller in a [Software Defined Network (SDN)](https://en.wikipedia.org/wiki/Software-defined_networking). Its purpose is to allow the implementation of additional functionality based on the interception and modification of existing control messages. The interception of messages at this point enables applications that cannot be implemented by a regular SDN application connected to the controller via the northbound interface. All messages exchanged between a switch and a controller can be monitored, filtered, modified and new messages can be introduced. For example this can be used to create a virtual SDN switch consisting of multiple physical switches or to detect and prevent attacks against the controller.

## What is the Enhanced Framework?
The enhanced framework is an enhanced version of an [existing framework for GCMI](https://github.com/kit-tm/gcmi). A GCMI framework implements a TCP proxy to intercept messages and forwards them to GCMI Apps. GCMI Apps are applications that can modify, monitor or drop all incoming messages. An arbitrary number of GCMI Apps can be plugged into the framework. The framework handles the interception, parsing, serializing and forwarding of OpenFlow messages so that this functionality does not need to be implemented in GCMI Apps. In comparison to the existing framework, the enhanced framework supports TLS and allows GCMI Apps to apply filters. Moreover, a single TCP proxy can be used by multiple GCMI Apps leading to an improved performance.
