/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.example.iapdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.graphics.Color
import com.amazon.device.iap.PurchasingService
import android.util.Log
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.model.UserDataResponse
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.example.iapdemo.databinding.ActivityMainBinding
import java.util.*

const val parentSKU = "techsubscription"

class MainActivity : AppCompatActivity() {

    private lateinit var currentUserId: String
    private lateinit var currentMarketplace: String
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        PurchasingService.registerListener(this, purchasingListener)
        Log.v("KOTLIN_INTEGRATION", "Registering PurchasingListener")

        binding.subscriptionButton.setOnClickListener { PurchasingService.purchase(parentSKU) }

    }

    override fun onResume() {
        super.onResume()

        //getUserData() will query the Appstore for the Users information
        PurchasingService.getUserData()

        //getPurchaseUpdates() will query the Appstore for any previous purchase
        PurchasingService.getPurchaseUpdates(true)

        //getProductData will validate the SKUs with Amazon Appstore
        val productSkus = hashSetOf("techquarterly","techmonthly")

        PurchasingService.getProductData(productSkus)
        Log.v("KOTLIN_INTEGRATION", "Validating SKUs with Amazon")
    }

    private var purchasingListener: PurchasingListener = object : PurchasingListener {
        override fun onUserDataResponse(response: UserDataResponse) {
            Log.v("KOTLIN_INTEGRATION", "onUserDataResponse")
            when (response.requestStatus) {
                UserDataResponse.RequestStatus.SUCCESSFUL -> {
                    currentUserId = response.userData.userId
                    currentMarketplace = response.userData.marketplace
                    Log.v("KOTLIN_INTEGRATION", response.userData.toString())
                }
                UserDataResponse.RequestStatus.FAILED, UserDataResponse.RequestStatus.NOT_SUPPORTED, null -> {
                    Log.e("KOTLIN_INTEGRATION", "Request error")
                }
            }
        }

        override fun onProductDataResponse(productDataResponse: ProductDataResponse) {
            Log.v("KOTLIN_INTEGRATION", "onProductDataResponse")
            when (productDataResponse.requestStatus) {
                ProductDataResponse.RequestStatus.SUCCESSFUL -> {
                    Log.v("KOTLIN_INTEGRATION", "ProductDataResponse.RequestStatus SUCCESSFUL")
                    val products = productDataResponse.productData
                    for (key in products.keys) {
                        val product = products[key]
                        Log.v(
                            "KOTLIN_INTEGRATION",
                            "Product: ${product!!.title} \n Type: ${product.productType}\n SKU: ${product.sku}\n Price: ${product.price}\n Description: ${product.description}\n"
                        )
                    }
                    for (s in productDataResponse.unavailableSkus) {
                        Log.v("KOTLIN_INTEGRATION", "Unavailable SKU:$s")
                    }
                }
                ProductDataResponse.RequestStatus.FAILED -> Log.v("KOTLIN_INTEGRATION", "ProductDataResponse.RequestStatus FAILED")

                else -> {
                    Log.e("KOTLIN_INTEGRATION", "Not supported")
                }
            }
        }

        override fun onPurchaseResponse(purchaseResponse: PurchaseResponse) {
            Log.v("KOTLIN_INTEGRATION", "onPurchaseResponse")
            when (purchaseResponse.requestStatus) {
                PurchaseResponse.RequestStatus.SUCCESSFUL -> {
                    Log.v("KOTLIN_INTEGRATION", "PurchaseResponse.RequestStatus SUCCESSFUL")
                    Log.v("KOTLIN_INTEGRATION", purchaseResponse.receipt.toString())
                    PurchasingService.notifyFulfillment(
                        purchaseResponse.receipt.receiptId,
                        FulfillmentResult.FULFILLED
                    )
                }
                PurchaseResponse.RequestStatus.FAILED -> {
                  Log.v("KOTLIN_INTEGRATION", "PurchaseResponse.RequestStatus FAILED")
                }
                else -> {
                    Log.e("KOTLIN_INTEGRATION", "Not supported")
                }
            }
        }

        override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
            Log.v("KOTLIN_INTEGRATION", "onPurchaseUpdatesResponse")
            when (response.requestStatus) {
                PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL -> {
                    Log.v("KOTLIN_INTEGRATION", "PurchaseUpdatesResponse.RequestStatus SUCCESSFUL")
                    for (receipt in response.receipts) {
                        Log.v("KOTLIN_INTEGRATION", receipt.toString())
                        if (!receipt.isCanceled) {
                            binding.textView.apply {
                                text = "SUBSCRIBED"
                                setTextColor(Color.RED)
                            }
                        }
                    }
                    if (response.hasMore()) {
                        PurchasingService.getPurchaseUpdates(true)
                    }
                }
                PurchaseUpdatesResponse.RequestStatus.FAILED -> {
                  Log.v("KOTLIN_INTEGRATION", "PurchaseUpdatesResponse.RequestStatus FAILED")
                }
                else -> {
                    Log.e("KOTLIN_INTEGRATION", "Not supported")
                }
            }
        }
    }
}
