package com.jaak.kyc.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaak.kyc.data.model.ErrorModel
import com.jaak.kyc.data.model.finish.FinishResponse
import com.jaak.kyc.data.model.livenessverify.LivenessVerifyRequest
import com.jaak.kyc.data.model.livenessverify.LivenessVerifyResponse
import com.jaak.kyc.data.model.otoverify.OtoVerifyRequest
import com.jaak.kyc.data.model.otoverify.OtoVerifyResponse
import com.jaak.kyc.domain.FinishUseCase
import com.jaak.kyc.domain.LivenessVerifyUseCase
import com.jaak.kyc.domain.OtoVerifyUseCase
import com.jaak.kyc.utils.Constants
import com.jaak.kyc.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
/**
 * Viewmodel de episodios
 */
@HiltViewModel
class DocumentBase64Model @Inject constructor(
    private val livenessVerifyUseCase: LivenessVerifyUseCase,
    private val otoVerifyUseCase: OtoVerifyUseCase,
    private val finishUseCase : FinishUseCase) : ViewModel() {

    val livenessVerifyResponse = MutableLiveData<LivenessVerifyResponse>()
    val otoVerifyResponse = MutableLiveData<OtoVerifyResponse>()
    val finishResponse = MutableLiveData<FinishResponse>()

    val errorModel = MutableLiveData<ErrorModel>()
    val isLoading = MutableLiveData<Boolean>()
    /**
     * metodo para llamar al servicio de obtemer episodios
     */
    fun livenessVerify(request: LivenessVerifyRequest) {
        viewModelScope.launch {
            isLoading.postValue(true)
            val livenessVerify = livenessVerifyUseCase(Constants.TOKEN, request)
            if(livenessVerify.code() != 200){
                errorModel.value = Utils.responseBodyToResultsModel(livenessVerify.errorBody()!!)
                isLoading.postValue(false)
            }else{
                livenessVerifyResponse.value = livenessVerify.body()
            }
        }
    }
    /**
     * metodo para llamar al servicio de obtemer episodios
     */
    fun otoVerify(request: OtoVerifyRequest) {
        viewModelScope.launch {
            isLoading.postValue(true)
            val otoVerify = otoVerifyUseCase(Constants.TOKEN, request)
            if(otoVerify.code() != 200){
                errorModel.value = Utils.responseBodyToResultsModel(otoVerify.errorBody()!!)
                isLoading.postValue(false)
            }else{
                otoVerifyResponse.value = otoVerify.body()
            }
        }
    }
    /**
     * metodo para llamar al servicio de obtemer episodios
     */
    fun finish() {
        viewModelScope.launch {
            isLoading.postValue(true)
            val finish = finishUseCase(Constants.TOKEN)
            if(finish.code() != 200){
                errorModel.value = Utils.responseBodyToResultsModel(finish.errorBody()!!)
            }else{
                finishResponse.value = finish.body()
            }
            isLoading.postValue(false)
        }
    }
}