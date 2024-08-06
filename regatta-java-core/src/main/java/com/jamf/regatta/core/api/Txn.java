/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.api;

import com.jamf.regatta.core.api.op.Cmp;
import com.jamf.regatta.core.api.op.Op;

public interface Txn {

    /**
     * takes a list of comparison. If all comparisons passed in succeed,
     * the operations passed into Then() will be executed. Or the operations
     * passed into Else() will be executed.
     *
     * @param cmps the comparisons
     * @return this object
     */
    Txn If(Cmp... cmps);

    /**
     * takes a list of operations. The Ops list will be executed, if the
     * comparisons passed in If() succeed.
     *
     * @param ops the operations
     * @return this object
     */
    Txn Then(Op... ops);

    /**
     * takes a list of operations. The Ops list will be executed, if the
     * comparisons passed in If() fail.
     *
     * @param ops the operations
     * @return this object
     */
    Txn Else(Op... ops);

    /**
     * tries to commit the transaction.
     *
     * @return a TxnResponse
     */
    TxnResponse commit();
}
