/**
 * Ports of the accounts context — two kinds, and the difference decides who may
 * import what (ADR-003 decision 2).
 *
 * <p><b>Outbound</b>, called by this context and implemented in its own
 * infrastructure: {@link com.bankapp.accounts.application.port.DomainEventPublisher}.
 * Internal plumbing; nothing outside accounts has a reason to touch it.
 *
 * <p><b>Published inbound</b>, called by other contexts and implemented here:
 * {@link com.bankapp.accounts.application.port.AccountLedger} — move money
 * between two accounts, so payments can run a transfer without reaching the
 * Account aggregate or restating the rules that guard a balance.
 *
 * <p>This package is the <em>only</em> part of accounts another context may
 * depend on; its domain, infrastructure and api stay private, enforced by
 * ArchitectureTest. Adding an interface here is therefore a public commitment —
 * name it in accounts' own vocabulary, not the caller's, or the next consumer
 * arrives to find a method named after somebody else's use case.
 *
 * <p>Switch trigger: if the two kinds start getting confused, split the inbound
 * ones into a dedicated {@code contract} package and point the ArchUnit rule at
 * that instead.
 */
package com.bankapp.accounts.application.port;
