// SPDX-License-Identifier: MIT
package com.winkreyes.shops;
import com.winkreyes.shops.checkout.CheckoutService.MoneyResult;
import net.milkbowl.vault.economy.EconomyResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ShopAdapterTest {
    @Test void exactSuccessfulAmountIsAccepted(){assertEquals(MoneyResult.SUCCESS,ShopAdapter.result(new EconomyResponse(1.23,40,EconomyResponse.ResponseType.SUCCESS,""),123));}
    @Test void missingResponseIsUncertain(){assertEquals(MoneyResult.UNCERTAIN,ShopAdapter.result(null,123));}
    @Test void definitiveZeroFailureAllowsCompensation(){assertEquals(MoneyResult.DECLINED,ShopAdapter.result(new EconomyResponse(0,40,EconomyResponse.ResponseType.FAILURE,"declined"),123));}
    @Test void reportedAmountOnFailureNeverTriggersBlindRefund(){assertEquals(MoneyResult.UNCERTAIN,ShopAdapter.result(new EconomyResponse(1.23,40,EconomyResponse.ResponseType.FAILURE,"ambiguous"),123));}
    @Test void wrongSuccessfulAmountIsUncertain(){assertEquals(MoneyResult.UNCERTAIN,ShopAdapter.result(new EconomyResponse(1.2,40,EconomyResponse.ResponseType.SUCCESS,""),123));}
    @Test void nonfiniteResponsesAreUncertain(){for(double n:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}){assertEquals(MoneyResult.UNCERTAIN,ShopAdapter.result(new EconomyResponse(n,40,EconomyResponse.ResponseType.SUCCESS,""),123));assertEquals(MoneyResult.UNCERTAIN,ShopAdapter.result(new EconomyResponse(1.23,n,EconomyResponse.ResponseType.SUCCESS,""),123));}}
    @Test void unsupportedZeroResponseIsDeclined(){assertEquals(MoneyResult.DECLINED,ShopAdapter.result(new EconomyResponse(0,40,EconomyResponse.ResponseType.NOT_IMPLEMENTED,""),123));}
}
