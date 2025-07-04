package com.animecharacter.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.animecharacter.R
import com.animecharacter.models.Character
import com.google.android.material.card.MaterialCardView

class CharacterSelectionAdapter(
    private val onCharacterSelected: (Character) -> Unit
) : RecyclerView.Adapter<CharacterSelectionAdapter.CharacterViewHolder>() {

    private var characters = listOf<Character>()
    private var selectedCharacterName = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_character_selection, parent, false)
        return CharacterViewHolder(view)
    }

    override fun onBindViewHolder(holder: CharacterViewHolder, position: Int) {
        val character = characters[position]
        holder.bind(character, character.name == selectedCharacterName)
    }

    override fun getItemCount(): Int = characters.size

    fun updateCharacters(newCharacters: List<Character>, selectedName: String) {
        characters = newCharacters
        selectedCharacterName = selectedName
        notifyDataSetChanged()
    }

    fun updateSelectedCharacter(selectedName: String) {
        val oldSelectedIndex = characters.indexOfFirst { it.name == selectedCharacterName }
        val newSelectedIndex = characters.indexOfFirst { it.name == selectedName }
        
        selectedCharacterName = selectedName
        
        if (oldSelectedIndex != -1) {
            notifyItemChanged(oldSelectedIndex)
        }
        if (newSelectedIndex != -1) {
            notifyItemChanged(newSelectedIndex)
        }
    }

    inner class CharacterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val characterCard: MaterialCardView = itemView.findViewById(R.id.characterCard)
        private val characterImage: ImageView = itemView.findViewById(R.id.characterImage)
        private val characterName: TextView = itemView.findViewById(R.id.characterName)
        private val characterDescription: TextView = itemView.findViewById(R.id.characterDescription)
        private val selectedIndicator: View = itemView.findViewById(R.id.selectedIndicator)
        private val premiumBadge: View = itemView.findViewById(R.id.premiumBadge)
        private val lockedOverlay: View = itemView.findViewById(R.id.lockedOverlay)

        fun bind(character: Character, isSelected: Boolean) {
            characterName.text = character.name
            characterDescription.text = character.getShortDescription()
            characterImage.setImageResource(character.imageRes)
            
            // إظهار/إخفاء مؤشر الاختيار
            selectedIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE
            
            // إظهار/إخفاء شارة البريميوم
            premiumBadge.visibility = if (character.isPremium) View.VISIBLE else View.GONE
            
            // إظهار/إخفاء طبقة القفل
            lockedOverlay.visibility = if (!character.isUnlocked) View.VISIBLE else View.GONE
            
            // تطبيق التصميم المناسب
            if (isSelected) {
                characterCard.strokeColor = itemView.context.getColor(R.color.primary_color)
                characterCard.strokeWidth = 4
            } else {
                characterCard.strokeColor = itemView.context.getColor(R.color.surface_variant)
                characterCard.strokeWidth = 1
            }
            
            // إعداد النقر
            characterCard.setOnClickListener {
                if (character.isUnlocked) {
                    onCharacterSelected(character)
                } else {
                    // يمكن إظهار رسالة أو فتح شاشة الشراء
                    showUnlockDialog(character)
                }
            }
            
            // تطبيق الرسوم المتحركة
            itemView.alpha = 0f
            itemView.animate()
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(adapterPosition * 100L)
                .start()
        }
        
        private fun showUnlockDialog(character: Character) {
            androidx.appcompat.app.AlertDialog.Builder(itemView.context)
                .setTitle("شخصية مقفلة")
                .setMessage("هذه الشخصية غير متاحة حالياً. سيتم إضافتها في التحديثات القادمة.")
                .setPositiveButton("موافق", null)
                .show()
        }
    }
}

