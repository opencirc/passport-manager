# Passport Manager

A digital product passport platform for circular construction materials, managing building component assemblies, environmental classifications, and machine-authenticated verification audits.

## Language

### Governance & Identity

**Admin**:
A human user authorized to configure system infrastructure, projects, and access groups.
_Avoid_: User, Member, Account

**Owner**:
An organization or corporate entity that commissions and owns construction projects.
_Avoid_: Tenant, Client, Customer

**Project**:
A distinct physical building, construction development, or material workspace.
_Avoid_: Workspace, Facility, Site

**Access Group**:
A policy profile within a project that defines operational CRUD permissions for machine integrations.
_Avoid_: Role, Permission Set, Group

**API Key**:
A machine authentication token belonging to an access group used by external software to operate on domain data.
_Avoid_: Token, Secret, Bearer Key

### Material Passports

**Passport**:
A digital representation of a physical building element, assembly, or construction material.
_Avoid_: Material, Component, Product, Asset

**Root Passport**:
The top-level passport anchor that directly represents the building or project entity.
_Avoid_: Parent Passport, Project Root, Master Passport

**Child Passport**:
A constituent material or sub-assembly contained within a parent passport hierarchy.
_Avoid_: Sub-passport, Leaf Passport, Component

### Technical Specifications & Datasheets

**Datasheet Definition**:
A standardized classification specification cached from an external data dictionary like bSDD or buildingSMART Lexicon.
_Avoid_: Template, Schema, Class, Dictionary Entry

**Datasheet Definition Property**:
A specific attribute definition belonging to a datasheet definition.
_Avoid_: Property, Field, Specification Attribute

**Passport Datasheet**:
An instance attachment binding a datasheet definition and specific property values to a passport.
_Avoid_: Datasheet, Mapping, Property Set, Passport Property

### Audit & Verification

**Audit Log**:
An immutable record capturing an RFC 6902 state change delta applied to a passport, datasheet, or definition.
_Avoid_: History, Log Entry, Change Record, Event

**Audit Log Tag**:
A validated state endorsement attached to an audit log entry signed by a verifier and API key.
_Avoid_: Signature, Verification, Approval, Badge
