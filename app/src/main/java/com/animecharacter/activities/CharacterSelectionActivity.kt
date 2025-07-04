package com.animecharacter.activities

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.animecharacter.R
import com.animecharacter.adapters.CharacterSelectionAdapter
import com.animecharacter.databinding.ActivityCharacterSelectionBinding
import com.animecharacter.models.Character
import com.animecharacter.utils.PreferencesHelper

class CharacterSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCharacterSelectionBinding
    private lateinit var preferencesHelper: PreferencesHelper
    private lateinit var characterAdapter: CharacterSelectionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCharacterSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesHelper = PreferencesHelper(this)
        
        setupToolbar()
        setupRecyclerView()
        loadCharacters()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.character_selection_title)
        }
    }

    private fun setupRecyclerView() {
        characterAdapter = CharacterSelectionAdapter { character ->
            selectCharacter(character)
        }
        
        binding.charactersRecyclerView.apply {
            layoutManager = GridLayoutManager(this@CharacterSelectionActivity, 2)
            adapter = characterAdapter
        }
    }

    private fun loadCharacters() {
        val characters = getAvailableCharacters()
        val selectedCharacter = preferencesHelper.getSelectedCharacter()
        
        characterAdapter.updateCharacters(characters, selectedCharacter)
    }

    private fun getAvailableCharacters(): List<Character> {
        return listOf(
            Character(
                name = "ساكورا",
                description = "ساكورا هارونو - طبيبة نينجا ماهرة وصديقة مخلصة من أنمي ناروتو",
                imageRes = R.drawable.sakura_character,
                animationRes = R.raw.sakura_animation,
                personality = "ودودة ومهتمة، تحب مساعدة الآخرين وتقديم النصائح الطبية",
                voiceStyle = "صوت أنثوي لطيف ومشجع"
            ),
            Character(
                name = "ناروتو",
                description = "ناروتو أوزوماكي - نينجا متفائل ومصمم على تحقيق أحلامه",
                imageRes = R.drawable.naruto_character,
                animationRes = R.raw.naruto_animation,
                personality = "متحمس ومتفائل، يحب المغامرات ولا يستسلم أبداً",
                voiceStyle = "صوت ذكوري حماسي ومليء بالطاقة"
            ),
            Character(
                name = "لوفي",
                description = "مونكي دي لوفي - قبطان قراصنة مرح ومغامر من أنمي ون بيس",
                imageRes = R.drawable.luffy_character,
                animationRes = R.raw.luffy_animation,
                personality = "مرح وبسيط، يحب الطعام والمغامرات والأصدقاء",
                voiceStyle = "صوت ذكوري مرح وبسيط"
            ),
            Character(
                name = "غوكو",
                description = "سون غوكو - محارب قوي وطيب القلب من أنمي دراغون بول",
                imageRes = R.drawable.goku_character,
                animationRes = R.raw.goku_animation,
                personality = "طيب القلب وقوي، يحب القتال والتدريب والطعام",
                voiceStyle = "صوت ذكوري قوي ومتحمس"
            )
        )
    }

    private fun selectCharacter(character: Character) {
        // حفظ الشخصية المختارة
        preferencesHelper.setSelectedCharacter(character.name)
        
        // تحديث المحول
        characterAdapter.updateSelectedCharacter(character.name)
        
        // إظهار رسالة تأكيد
        binding.selectionConfirmationText.text = 
            getString(R.string.character_selected_message, character.name)
        binding.selectionConfirmationText.visibility = android.view.View.VISIBLE
        
        // إخفاء الرسالة بعد 3 ثوانٍ
        binding.selectionConfirmationText.postDelayed({
            binding.selectionConfirmationText.visibility = android.view.View.GONE
        }, 3000)
        
        // إرسال النتيجة للنشاط الرئيسي
        setResult(RESULT_OK)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

