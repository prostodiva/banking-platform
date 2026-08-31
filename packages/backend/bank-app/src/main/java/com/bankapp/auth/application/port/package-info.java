/**
 * Ports of the auth context. All five are <b>outbound</b> — called by this
 * context, implemented in its own infrastructure. Auth publishes no inbound port
 * yet: other contexts consume identity through the security filter chain and the
 * token, not through a Java interface.
 *
 * <p>Switch trigger: the first context that needs to ask a question about a user
 * (reports rendering "opened by") gets a published port here, named in auth's
 * vocabulary — not a peek at UserRepository.
 */
package com.bankapp.auth.application.port;
