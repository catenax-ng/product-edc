
# Audit Logging

## User Story
As a security auditor I can find all audit relevant actions in a dedicated audit log. The audit log is represented by chronological records documenting every administrative activitiy as well as potential malicious end user activities. The records are created as part of an EDC end-user or system interaction which ensured reliable and tamper-proof writing of each record.
Main purpose of the log is to enable the identification of malicious activities.

As a security officer I can use the auditlog as forensic evidence for legal proceedings.

## Requirements
Req1: Audit relevant actions should be logged. Audit relevant actions are defined as:
- Any modifying activity performed by the data management API has to be considered as configuration change and hence should be audit logged.
   - Create/Update/Delete Asset
   - Create/Update/Delete Policy
   - Create/Update/Delete Contractdefinition
- Potential malicious activities. This could be as easy as identifying the number of transfer processes created in a certain timeframe which could hint to a DoS attack.
   - Contract Negotiation (initial and final state)
   - Transfer Process (initial and final state)
   - AuthenticationService -> Failed authentication attempts (e.g. brute force attack detection would be enabled by this)

Req2: An audit log record should contain at least the following information
- Unique Identifier for the event (e.g. CreateAsset)
- UserId (Who performed the action)
- timestamp (ISO 8601, at least on one second interval)
- IP/Hostname (in case of external access, if feasible)
- Description to understand the event

Req3: The audit log should be available as service in core EDC as well as to every extension

Req4: The audit log should come with a default implementation, but it should be extensible, so that you can build the audit log persistence layer matching your needs, e.g. calling an external service, write to some file storage, slf4j logging.

Req5: An audit log relevant action must not be performed in case the audit log cannot be written (e.g. failed)

Req6: The audit log must not be exposed to any user. To access special permissions should be required.

Req7: Secrets, credentials, security tokens must not be contained in the audit log.

Req8: The audit log should be executed just before the user's action is executed. At the same time it should still be done on appropriate layer which covers the logic of the actions and does not have to be propagated to low level code. E.g. in case of creation of a new asset the audit log should happen just before the sql query is send to the database, the log will be performed by the assetAPIController, but not the sql extension.

## Out of scope requirements
- The audit log should not be used for metering with billing background
- Audit log retention period should be handled within the respective extension providing the persistence.
- End user access to the audit log should be handled in the extension providing the persistence.

## Implementation Options

### Enhance Monitor
Existing monitor Service could be enhanced by additional method
````java

public interface Monitor {
...
    default void audit(AuditInformation auditInfo) {
    }

    default void audit(String message) {
    }
}
````
The corresponding implementation could take care of specific handling of audit().

Current monitor service is already used in most of the extensions and hence adopting auditlog would cause minimal effort.

### Add Auditlog Service
Template for the new service would be the monitor. But to express the difference of audit and application log the reuse of the monitor service avoided.
````java
public interface AuditLogger {

    default void log(AuditInformation auditInfo) {

    }

    default void log(String message) {
    }

}
````
Prototype can be found under: https://github.com/sap-contributions/DataSpaceConnector/tree/auditLogAsService

The Monitor and Telemetry services have been exposed in the same way and hence AuditLogger would be a perfect match for this.

### Use event framework
Background for event framework: https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/tree/b71a14c44f3718ce4a2c9b40769bc70d863716ad/docs/developer/decision-records/2022-06-03-event-framework

Some actions offer already implemented event hooks, e.g. TransferProcessListener. These event hooks can be used to emit audit log records.
Do do so the audit log extentions would implement a `EventSubscriber` and trigger the persistence inside the handler.

````java
public class EventAuditLogger implements EventSubscriber {
    @Override
    public void on(Event<?> event) {
        
    }
}
````

As of now only a couple of EDC actions already support the event framework. The effort for adoption inside an extension (compare Req3) seems to be quite high.
The current design of the event framework would lead to potential audit log losses as the subscriber is asynchronous. There are discussions to overcome the limitation and introduce a synchronous subscriber which would enable a transactional behaviour for persistence activities in case an audit subscriber throws and exception.