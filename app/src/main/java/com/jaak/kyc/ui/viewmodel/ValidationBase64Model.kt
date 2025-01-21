package com.jaak.kyc.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaak.kyc.data.model.ErrorModel
import com.jaak.kyc.data.model.ocr.DocumentExtraBothRequest
import com.jaak.kyc.data.model.ocr.DocumentExtraBothResponse
import com.jaak.kyc.data.model.verify.VerifyRequest
import com.jaak.kyc.data.model.verify.VerifyResponse
import com.jaak.kyc.domain.OcrUseCase
import com.jaak.kyc.domain.VerifyUseCase
import com.jaak.kyc.utils.Constants
import com.jaak.kyc.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
/**
 * Viewmodel de episodios
 */
@HiltViewModel
class ValidationBase64Model @Inject constructor(
    private val verifyUseCase: VerifyUseCase,
    private val ocrUseCase: OcrUseCase) : ViewModel() {

    val verifyResponse = MutableLiveData<VerifyResponse>()
    val documentExtraBothResponse = MutableLiveData<DocumentExtraBothResponse>()

    val errorModel = MutableLiveData<ErrorModel>()
    val isLoading = MutableLiveData<Boolean>()
    /**
     * metodo para llamar al servicio de obtemer episodios
     */
    fun verify(request: VerifyRequest) {
        viewModelScope.launch {
            isLoading.postValue(true)
            val verify = verifyUseCase(Constants.TOKEN, request)
            if(verify.code() != 200){
                errorModel.value = Utils.responseBodyToResultsModel(verify.errorBody()!!)
                isLoading.postValue(false)
            }else{
                verifyResponse.value = verify.body()
            }
        }
    }

    /**
     * metodo para llamar al servicio de obtemer episodios
     */
    fun ocr(request: DocumentExtraBothRequest) {
        viewModelScope.launch {
            isLoading.postValue(true)
            val ocr = ocrUseCase(Constants.TOKEN, request)
            if(ocr.code() != 200){
                errorModel.value = Utils.responseBodyToResultsModel(ocr.errorBody()!!)
            }else{
                documentExtraBothResponse.value = ocr.body()
            }
            isLoading.postValue(false)
        }
    }
}