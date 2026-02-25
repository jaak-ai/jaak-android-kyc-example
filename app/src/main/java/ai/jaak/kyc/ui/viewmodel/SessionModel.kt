package ai.jaak.kyc.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.jaak.kyc.data.model.ErrorModel
import ai.jaak.kyc.data.model.session.SessionResponse
import ai.jaak.kyc.domain.SessionUseCase
import ai.jaak.kyc.utils.Constants
import ai.jaak.kyc.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
/**
 * Viewmodel de episodios
 */
@HiltViewModel
class SessionModel @Inject constructor(
    private val sessionUseCase: SessionUseCase)  : ViewModel() {

    val sessionResponse = MutableLiveData<SessionResponse>()
    val errorModel = MutableLiveData<ErrorModel>()
    val isLoading = MutableLiveData<Boolean>()
    val validation = MutableLiveData<String>()

    /**
     * metodo para llamar al servicio de obtemer episodios
     */
    fun session(shortKey: String) {
        viewModelScope.launch {
            isLoading.postValue(true)
            if(shortKey.isEmpty()){
                validation.value = ""
            }else{
                val session = sessionUseCase(shortKey, Constants.ORIGIN_DEVICE)
                if(session.code() != 200){
                    errorModel.value = Utils.responseBodyToResultsModel(session.errorBody()!!)
                }else{
                    sessionResponse.value = session.body()
                }
            }

            isLoading.postValue(false)
        }
    }
}