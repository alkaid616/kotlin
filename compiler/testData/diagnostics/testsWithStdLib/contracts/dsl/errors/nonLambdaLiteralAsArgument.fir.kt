// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -EXPOSED_PARAMETER_TYPE

import kotlin.contracts.*

fun passLambdaValue(l: ContractBuilder.() -> Unit) {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract(l)<!>
}

fun passAnonymousFunction(x: Boolean) {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract(fun ContractBuilder.() {
        returns() implies x
    })<!>
}
