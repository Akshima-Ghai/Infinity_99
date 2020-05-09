package com.pranjal.video.activities

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.annotation.StringRes

import com.pranjal.video.utils.EXTRA_QB_USERS_LIST
import com.pranjal.video.utils.SAMPLE_CONFIG_FILE_NAME
import com.pranjal.video.utils.getAllUsersFromFile
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.database.DatabaseReference
import com.hbb20.CountryCodePicker
import com.pranjal.video.R
import com.pranjal.video.utils.EXTRA_QB_USERS_LIST
import com.pranjal.video.utils.SAMPLE_CONFIG_FILE_NAME
import com.pranjal.video.utils.getAllUsersFromFile
import com.quickblox.auth.session.QBSettings
import com.quickblox.chat.QBChatService
import com.quickblox.core.LogLevel
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.exception.QBResponseException
import com.quickblox.users.QBUsers
import com.quickblox.users.model.QBUser
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    lateinit var sharedPreference: SharedPreferences
    private lateinit var etUsername: EditText
    private lateinit var btnLogin: Button

    private lateinit var etPassword: EditText
    private lateinit var etPhone: EditText

    private lateinit var country: CountryCodePicker
    private lateinit var database: DatabaseReference
    private lateinit var etCode: EditText
    private lateinit var btSendOtp: Button
    private lateinit var btResendOtp: Button
    private lateinit var btVerifyOtp: Button
    private lateinit var mAuth: FirebaseAuth
var count=0
    lateinit var mVerificationId: String
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    fun onCodeSent(
        verificationId: String?=null,
        token: PhoneAuthProvider.ForceResendingToken?
    ) {
        Toast.makeText(this@LoginActivity, "Code Sent", Toast.LENGTH_SHORT).show()
        if (verificationId != null) {
            mVerificationId = verificationId
        } //Add this line to save //verification Id
    }

    val TAG = LoginActivity::class.java.simpleName
    private lateinit var users: ArrayList<QBUser>
    private lateinit var adapter: ArrayAdapter<String>
    var progressDialog: ProgressDialog? = null
    private var opponents: ArrayList<QBUser>? = null
    private val qbChatService: QBChatService = QBChatService.getInstance()
    private val isLoggedIn: Boolean
        get() = QBChatService.getInstance().isLoggedIn


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        supportActionBar?.setTitle(R.string.title_login_activity)

        initChat()
        initQBUsers()
        initUserAdapter()
        sharedPreference = getSharedPreferences(
            getString(R.string.video_preference_file),
            Context.MODE_PRIVATE
        )
        var isLoggedIn = sharedPreference.getBoolean("isLoggedIn", false)






        btSendOtp = findViewById(R.id.bt_send_otp);
        btResendOtp = findViewById(R.id.bt_resend_otp);
        btVerifyOtp = findViewById(R.id.bt_verify_otp);


        etUsername = findViewById<EditText>(R.id.etUsername)
        etPassword = findViewById<EditText>(R.id.etPassword)
        btnLogin = findViewById<Button>(R.id.btnLogin)

        etPhone = findViewById(R.id.etPhone)

        country = findViewById(R.id.country)
        etCode = findViewById(R.id.etCode)


        mAuth = FirebaseAuth.getInstance()
        if (isLoggedIn) {
            startActivity(intent)
            finish()
        }


        var pass = etPassword.text.toString()
        var username = etUsername.text.toString()
        btSendOtp.setOnClickListener {
            verify()
        }
        btVerifyOtp.setOnClickListener {

            authenticate()

        }
        btResendOtp.setOnClickListener {
            verify()
        }
         fun onStart() {
            super.onStart()

            if (mAuth?.currentUser == null) {
                startActivity(intent)
            } else
                Toast.makeText(this, "Already signed in", Toast.LENGTH_LONG).show()


        }
    }

    private fun initQBUsers() {
        users = getAllUsersFromFile(SAMPLE_CONFIG_FILE_NAME, this)

    }

    private fun initUserAdapter() {
        val userList: ArrayList<String> = ArrayList(users.size)
        users.forEachIndexed { index, _ -> userList.add(users[index].login) }
        adapter = ArrayAdapter(this, R.layout.list_item_user, userList)
        list_users.adapter = adapter
        list_users.choiceMode = ListView.CHOICE_MODE_SINGLE
        list_users.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            loginToQB(users[position])
        }
    }


    private fun initChat() {
        QBSettings.getInstance().logLevel = LogLevel.DEBUG
        QBChatService.setDebugEnabled(true)
        QBChatService.setConfigurationBuilder(QBChatService.ConfigurationBuilder().apply { socketTimeout = 0 })
    }

    private fun startCallActivity() {
        val intent = Intent(this, CallActivity::class.java)
        intent.putExtra(EXTRA_QB_USERS_LIST, opponents)
        startActivity(intent)
    }


    private fun loginToQB(user: QBUser) {
        showProgress(R.string.dlg_login)
        QBUsers.signIn(user).performAsync(object : QBEntityCallback<QBUser> {
            override fun onSuccess(qbUser: QBUser, args: Bundle) {
                user.id = qbUser.id!!
                loginToChat(user)
            }

            override fun onError(ex: QBResponseException) {
                hideProgress()
                Toast.makeText(applicationContext, getString(R.string.login_chat_login_error, ex.message), Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun loginToChat(user: QBUser) {
        if (!isLoggedIn) {
            qbChatService.login(user, object : QBEntityCallback<Void> {
                override fun onSuccess(void: Void?, bundle: Bundle?) {
                    hideProgress()
                    loadUsers()
                }

                override fun onError(ex: QBResponseException) {
                    hideProgress()
                    Toast.makeText(applicationContext, getString(R.string.login_chat_login_error, ex.message), Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    fun loadUsers() {
        showProgress(R.string.dlg_loading_opponents)
        val usersLogins = ArrayList<String>()
        users.forEach { usersLogins.add(it.login) }
        QBUsers.getUsersByLogins(usersLogins, null).performAsync(object : QBEntityCallback<ArrayList<QBUser>> {
            override fun onSuccess(qbUsers: ArrayList<QBUser>, p1: Bundle?) {
                hideProgress()
                opponents = qbUsers
                startCallActivity()
            }

            override fun onError(ex: QBResponseException) {

                hideProgress()
                Toast.makeText(applicationContext, getString(R.string.loading_users_error, ex.message), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showProgress(@StringRes messageId: Int) {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this)
        }
        showProgressDialog(this, progressDialog!!, messageId)
    }

    private fun hideProgress() {
        progressDialog?.dismiss()
    }

    private fun showProgressDialog(context: Context, progressDialog: ProgressDialog, @StringRes messageId: Int) {
        progressDialog.isIndeterminate = true
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)

        progressDialog.setMessage(context.getString(messageId))

        progressDialog.show()
    }
    private fun verify()
    {
        verifyCallbacks()
        var number = etPhone.text.toString()
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            number, // Phone number to verify
            60, // Timeout duration
            java.util.concurrent.TimeUnit.SECONDS, // Unit of timeout
            this, // Activity (for callback binding)
            callbacks

        )

    }

    private fun verifyCallbacks()
    {

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                {

                        signIn(credential)



                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(this@LoginActivity,
                    "click resend to receive another otp",
                    Toast.LENGTH_LONG)
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.


                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    // ...
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    // ...
                }

                // Show a message and update the UI
                // ...
            }

            override fun onCodeSent(verification:String?,p1:PhoneAuthProvider.ForceResendingToken?)
            {
                if (p1 != null) {
                    if (verification != null) {
                        super.onCodeSent(verification, p1)
                    }
                }
                mVerificationId=verification.toString()

                authenticate()
            }


        }

    }
    private fun authenticate()
    { var code=etCode.text.toString()
        val  credential:PhoneAuthCredential =PhoneAuthProvider.getCredential(mVerificationId,code)
        signIn(credential)
    }



    private fun signIn(credential: PhoneAuthCredential)
    {
        mAuth.signInWithCredential(credential).addOnCompleteListener{
                task: Task<AuthResult> ->  if(task.isSuccessful)
        {toast("Logged in Successfully")
            startActivity(intent)}

        }
    }

    private fun toast(msg:String)
    {
        Toast.makeText(this@LoginActivity,msg,Toast.LENGTH_SHORT).show()
    }
    private fun  savePreference(title:String)
    { var title=etUsername.text.toString()
        sharedPreference.edit().putBoolean("isLoggedIn",true).apply()
        sharedPreference.edit().putString("title",title).apply()

    }
    override fun onStop() {
        super.onStop()
        count++
        if(count>=3)
        { toast("Do not press home button while an exam .if you presss it once again your exam will be terminated")
        finish()}
    }
}






