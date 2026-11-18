# Model comparison

- **gemma-4-12b-Q5_K_M** (gemma-4-12b-it-Q5_K_M.gguf): load 5.6 s, avg 3.3 s per line, 59 tokens/s
- **gemma-4-12b-Q6_K** (gemma-4-12b-it-Q6_K.gguf): load 18.9 s, avg 3.6 s per line, 53 tokens/s

## archaic-1 (archaic speech (old mountain guide))

> そなたは旅の者か。この峠を夜に越えようとは、命が惜しくないと見える。

### gemma-4-12b-Q5_K_M (3.64 s)

TRANSLATION: Are you a traveler? Attempting to cross this pass at night suggests that you don't value your life very much.
LITERAL: You [polite/archaic] traveler [subject marker] are [question]? This pass [object marker] night [at] cross [volitional] to, life [subject marker] dear [not] to look.
NOTES:
- The speaker uses archaic/formal speech, likely a fantasy setting or a samurai-era character.
- そなた is an archaic way to say "you," often used by older characters or those in positions of authority.
- 命が惜しくない (inochi ga oshiku nai) is a common set phrase meaning "to be reckless" or "to have a death wish."
- ～ようとは indicates a sense of surprise or judgment regarding the listener's intention to do something.

### gemma-4-12b-Q6_K (4.58 s)

TRANSLATION: Are you a traveler? Attempting to cross this pass at night suggests you have little regard for your life.
LITERAL: You (honorific/archaic) are traveler's person. This pass [object] night [at] cross-try-to, life [subject] precious-not-to-be seen.
NOTES:
- The tone is archaic and formal, typical of a fantasy setting or a samurai-era character.
- そなた is an archaic way to say "you," often used by older characters or those in positions of authority.
- 命が惜しくない (inochi ga oshiku nai) is a set phrase meaning "to be reckless" or "to not value one's life."
- ～とは is used here to set the topic of the sentence, essentially saying "As for the fact that you are trying to cross this pass at night..."
- 見える is used here to mean "it seems" or "it appears," rather than the physical act of seeing.


## archaic-2 (archaic speech, needs context)

> わしを侮るでない。この山の道は、五十年歩いてきたこの足が覚えておるのじゃ。

### gemma-4-12b-Q5_K_M (3.72 s)

TRANSLATION: Don't underestimate me. These feet of mine, which have walked these mountain paths for fifty years, remember them well.
LITERAL: Me [object marker] underestimate do not. This mountain's path [topic marker] fifty years walked [past tense] this feet [subject marker] remember [polite/archaic] is.
NOTES:
- The speaker uses an elderly, authoritative, and slightly archaic style, indicated by the pronouns わし (washi) and the sentence-ending particle のじゃ (no ja).
- 侮るでない (asuru denai) is a firm, slightly old-fashioned way to say "don't underestimate/scorn."
- The grammar structure ...てきた (...tekita) indicates an action that has continued up until the present moment (walking for 50 years).
- The subject of the first part is the listener (implied), and the subject of the second part is the speaker's feet (この足).

### gemma-4-12b-Q6_K (4.72 s)

TRANSLATION: Don't underestimate me. These mountain paths are remembered by these feet of mine, which have walked for fifty years.
LITERAL: Me [object marker] underestimate do not. This mountain's path [topic marker] this mountain's path [subject marker] fifty years walked-have [past tense] this feet [subject marker] remembering is [polite/archaic ending].
NOTES:
- The speaker uses an elderly, authoritative, and slightly archaic style (indicated by わし and the sentence-ending のじゃ).
- 侮る (ashiru) means to underestimate or insult; the negative command ではない/でない is used here to mean "do not."
- The structure ...足が覚えておる (ashi ga oboete oru) uses the auxiliary verb おる, which is a humble or regional/archaic version of いる (to be/exist), often used in "remembering" to show a state of being.
- The subject of "walked" (歩いてきた) is the speaker's feet (足), but it implies the speaker's entire life experience.


## archaic-3 (archaic speech, omitted subject)

> 礼には及ばぬ。代わりに、麓の村まで薬を届けてもらえれば、それでよい。

### gemma-4-12b-Q5_K_M (3.48 s)

TRANSLATION: No need for thanks. Instead, if you could just deliver the medicine to the village at the foot of the mountain, that would be enough.
LITERAL: Thanks to is not reach. Instead, foot's village until medicine to deliver if you get, with that, by that is good.
NOTES:
- The speaker is using a polite but slightly archaic/formal tone, consistent with an older character (indicated by the previous use of わし and のじゃ).
- 礼には及ばぬ is a set phrase meaning "don't mention it" or "no need for thanks." It literally means "it doesn't reach the level of [requiring] thanks."
- 麓 (foot of a mountain) is a specific geographical term often used in fantasy or rural settings.
- The grammar structure ...れば、それでよい indicates a conditional "if [action], then that is sufficient."

### gemma-4-12b-Q6_K (4.19 s)

TRANSLATION: No need for thanks. Instead, if you could just deliver the medicine to the village at the foot of the mountain, that would be enough.
LITERAL: Thanks to (as for) not reach. Instead, foot's village until medicine (object) deliver let me do, with that, by that is good.
NOTES:
- The speaker is using an archaic/formal style (わし, じゃ, 届けてもらえれば), consistent with an older man or a fantasy setting.
- 礼には及ばぬ is a set phrase meaning "No need for thanks" or "Don't mention it." It literally means "It doesn't reach the level of [needing] thanks."
- 届けてもらえれば is a conditional form of 届けてもらう (to have someone deliver something). It expresses a polite request or a condition for the favor.
- 麓 (fumoto) specifically refers to the base or foot of a mountain.


## merchant-1 (polite merchant bargaining)

> この毛皮なら銀貨二十枚が相場ですが、今日は特別に十八枚でお譲りしましょう。

### gemma-4-12b-Q5_K_M (3.69 s)

TRANSLATION: The market price for this fur is twenty silver coins, but today I'll let you have it for eighteen as a special deal.
LITERAL: This fur if, silver coins twenty pieces [is] market price, but, today specially eighteen pieces for [to] hand over [I] shall do.
NOTES:
- The speaker is using polite, formal speech (desu/masu), likely a merchant or a shopkeeper.
- お譲りしましょう (oyuzuri shimashou) is a humble/polite way to say "I will give/sell it to you." It literally means "let me hand it over," making the transaction sound like a favor.
- 相場 (souba) refers to the current "going rate" or "market price."
- The grammar structure [Noun] なら (nara) is used here to set the condition: "If it's this [item]..." or "As for this [item]..."

### gemma-4-12b-Q6_K (3.93 s)

TRANSLATION: The market price for this fur is twenty silver coins, but today I'll let you have it for eighteen as a special deal.
LITERAL: This fur if, silver coins twenty pieces market price is, but today specially eighteen pieces for [to] hand over [I] shall do.
NOTES:
- The speaker is using polite but business-like speech (desu/masu), likely a merchant or a shopkeeper.
- お譲りしましょう (oyuzuri shimashou) is a humble/polite way to say "I will give/sell it to you." It literally means "let me hand it over."
- 相場 (souba) refers to the current market price or the standard going rate for an item.
- The particle なら (nara) is used here to set the condition: "If it's this fur [we are talking about]..."


## merchant-2 (idiom (足元を見る), casual merchant)

> 足元を見られちゃ困りますな。こっちも生活がかかってるんでね。

### gemma-4-12b-Q5_K_M (3.79 s)

TRANSLATION: You can't go around trying to lowball me like that. My livelihood depends on this, too.
LITERAL: Feet looked at [being] seen is troubled, isn't it. This side also life is hanging, so.
NOTES:
- The speaker uses a slightly rough, masculine, and somewhat rural or "old-fashioned" tone, indicated by the sentence-ending particles 「な」 and 「ね」.
- 「足元を見る」 is an idiom meaning "to take advantage of someone" or "to look down on someone's position" (literally "to look at someone's feet").
- 「〜ちゃ」 is a contraction of 「〜ては」, used here to express a sense of regret or "it would be a problem if..."
- 「こっち」 refers to the speaker's side/position/business.
- 「かかってる」 (from かかる) in this context means "is at stake" or "depends on."

### gemma-4-12b-Q6_K (3.7 s)

TRANSLATION: You can't go around trying to lowball me like that. My livelihood depends on this, too.
LITERAL: Feet looked at [being] seen is troubled, isn't it. This side also life is hanging, so.
NOTES:
- The tone is slightly rough but composed, typical of a merchant or a street-smart character. The use of ～ね at the end adds a persuasive, explanatory nuance.
- 足元を見る is an idiom meaning "to take advantage of someone" or "to look down on someone's position" (literally "to look at someone's feet").
- ～ちゃ is a contraction of ～ては, used here to express that a situation is problematic or regrettable.
- こっち is used here to mean "my side" or "me," referring to the speaker's own situation or business.


## merchant-3 (proverb)

> 損して得取れ、ってやつですよ。

### gemma-4-12b-Q5_K_M (3.12 s)

TRANSLATION: It's that "lose little to gain much" kind of thing.
LITERAL: Lose and gain-take, [particle] thing it is.
NOTES:
- The tone is explanatory and slightly casual, using the polite ~desu ending.
- 損して得取れ (Son shite eru tore) is a common Japanese proverb. It literally means "suffer a loss and take a profit," similar to "cut your losses" or "investing small to gain big."
- ってやつ (tte yatsu) is a very common colloquial construction used to define or categorize something. It translates to "the thing called..." or "the kind of thing where..."
- The grammar structure して (shite) is the te-form of する, used here to connect two actions in a sequence.

### gemma-4-12b-Q6_K (3.22 s)

TRANSLATION: It's that saying, "Lose little to gain much."
LITERAL: Lose and gain, [particle] thing is.
NOTES:
- The tone is explanatory and slightly casual, using the polite 「です」 ending.
- 「損して得取れ」 (Son shite eru tore) is a common Japanese proverb. It suggests that making a small sacrifice or loss now will lead to a greater profit or benefit later.
- 「〜ってやつ」 is a common colloquial way to refer to a concept, saying, or "the thing called..."
- The verb 「取れ」 is the imperative form of 「取る」, but in proverbs, it functions as a command or a general rule rather than a direct order to the listener.


## rough-1 (rough male speech, contractions)

> だから言ったじゃねえか。あいつに任せときゃろくなことにならねえって。

### gemma-4-12b-Q5_K_M (3.18 s)

TRANSLATION: I told you so. Nothing good is going to happen if we leave it to that guy.
LITERAL: Therefore said wasn't it. That guy to leave if do good thing won't become.
NOTES:
- The tone is rough and masculine, typical of a "tough guy" or a frustrated friend.
- じゃねえか is a rough contraction of じゃなかったか (wasn't it?), used here to emphasize "I told you so."
- 任せときゃ is a contraction of 任せておけば (if [you] leave it [to him]). The ろくなことにならねえ is a common set phrase meaning "nothing good will happen."
- The subject is "I" (the speaker) and the implied subject of the second clause is "you" (the listener).

### gemma-4-12b-Q6_K (3.17 s)

TRANSLATION: That's what I told you. I said leaving it to that guy wouldn't end well.
LITERAL: Therefore said wasn't? That guy to leave if good thing won't become.
NOTES:
- The tone is rough and masculine, typical of a frustrated or aggressive character.
- じゃねえか is a rough contraction of じゃなかったか, used here to emphasize "I told you so."
- 任せときゃ is a contraction of 任せておけば (if you leave it to...).
- ろくなことにならねえ is a common set phrase meaning "it won't turn out well" or "it'll end in disaster." The ねえ is a rough contraction of ない.


## rough-2 (rough, omitted subject, set phrase)

> …まあ、今さら文句言っても始まんねえか。

### gemma-4-12b-Q5_K_M (2.65 s)

TRANSLATION: ...Well, there's no point in complaining now.
LITERAL: ...Well, now at this late stage, even if [I] complain, [it] won't start.
NOTES:
- The speaker is using a rough, masculine, and somewhat resigned tone.
- 始まんねえ is a rough contraction of 始まらない (shimaranai). In this context, it means "it won't change anything" or "it's pointless."
- 今さら is a common set phrase meaning "at this late stage" or "now, of all times."
- 文句 (monku) means a complaint or grumbling.

### gemma-4-12b-Q6_K (3.09 s)

TRANSLATION: ...Well, it's no use complaining now.
LITERAL: ...Well, now at this late stage, even if (I) say complaints, (it) won't start.
NOTES:
- The tone is informal and slightly resigned. The speaker is likely reacting to a situation that has already gone wrong.
- 始まんねえ is a rough contraction of 始まらない (shimaranai). In this context, it means "it won't change anything" or "it's pointless."
- 今さら is a common set phrase meaning "at this late stage" or "now, of all times."
- 文句 (monku) refers to complaints or grumbling.


## tsundere (tsundere stutter, sentence-final particles)

> べ、別にあんたのために作ったわけじゃないんだからね！

### gemma-4-12b-Q5_K_M (3.02 s)

TRANSLATION: I-It's not like I made this for you or anything!
LITERAL: No, not for you made [it] reason isn't from!
NOTES:
- The tone is "tsundere" style—defensive, slightly embarrassed, and informal. The use of べ (be) at the start indicates a stutter or hesitation.
- あんた (anta) is a familiar, sometimes slightly blunt way of saying "you."
- わけじゃない (wake janai) is a common construction meaning "it's not that..." or "it's not the case that..." used to deny a specific reason or motive.
- The sentence ends with ね (ne) to add an assertive, seeking-agreement, or slightly nagging nuance to the denial.

### gemma-4-12b-Q6_K (3.1 s)

TRANSLATION: I-it's not like I made this for you or anything!
LITERAL: No, not for you made (it) reason isn't so!
NOTES:
- The tone is "tsundere" style: defensive, slightly embarrassed, and informal. The use of べ (be) is a stuttering prefix.
- わけじゃない (wake ja nai) is a common construction meaning "it's not that..." or "it's not the case that..."
- あんた (anta) is an informal, sometimes slightly blunt way of saying "you."
- The sentence ends with ね (ne) to add an insistent, seeking-agreement, or defensive nuance to the denial.


## reading (reading choice: 明日 = あした in casual speech)

> 明日は朝早いから、もう寝なきゃ。

### gemma-4-12b-Q5_K_M (2.49 s)

TRANSLATION: I have to go to sleep now because I have to get up early tomorrow.
LITERAL: Tomorrow [as for] morning [is] early, so already [must] sleep.
NOTES:
- The tone is casual and conversational, typical of friends or family.
- なきゃ is a common contraction of なければならない (must/have to). It is very frequent in spoken Japanese.
- 朝早い (asa hayai) is a set phrase meaning "to have an early start" or "to wake up early."
- The subject is "I," which is omitted as is standard in Japanese when the context is clear.

### gemma-4-12b-Q6_K (2.85 s)

TRANSLATION: I have to go to sleep now because I have to get up early tomorrow.
LITERAL: Tomorrow [as for] morning early because, already sleep must-do.
NOTES:
- The tone is casual and conversational, typical of a friend or family member speaking.
- 寝なきゃ is a common contraction of 寝なければならない (must sleep). The ～なきゃ ending is very frequent in spoken Japanese to express necessity.
- 朝早い (asa hayai) is a set phrase meaning "early in the morning" or "starting early tomorrow."
- The subject is "I," which is omitted as is standard in Japanese when the context is clear.


## ocr-error (OCR mistake: 間遺って should be 間違って)

> お前の言う通りだ、俺が間遺っていた。

### gemma-4-12b-Q5_K_M (3.31 s)

OCR: お前の言う通りだ、俺が間違っていた。

TRANSLATION: You're right, I was wrong.
LITERAL: Your saying's way is, I mistake was.
NOTES:
- Tone: Masculine and informal. The use of 俺 (ore) and the lack of polite endings indicate a close relationship or a rougher masculine speech style.
- 間違っていた: This is the past continuous/progressive form of 間違う (to make a mistake). It implies "I was in a state of being wrong" or "I was mistaken."
- 言う通り: A common set phrase meaning "just as [someone] says" or "exactly as [someone] said."
- Correction: The OCR "間遺" was corrected to "間違" (mistake), as "間遺" is not a standard word in this context.

### gemma-4-12b-Q6_K (3.14 s)

TRANSLATION: You're right, I was being careless.
LITERAL: Your saying's way is, I was careless-ing.
NOTES:
- The speaker is using masculine, informal speech (俺, お前).
- 間遺っていた is likely a recognition error for 見落としていた (missed/overlooked) or 間違っていた (was wrong). Given the context of "you're right," it most likely means "I made a mistake" or "I overlooked it."
- お前の言う通りだ is a common set phrase meaning "It's just as you say" or "You're right."
- The past continuous form 〜ていた indicates a state or action that was ongoing in the past.


## idiom (idiom (猫の手も借りたい))

> 猫の手も借りたいほど忙しいんだ、手伝ってくれ。

### gemma-4-12b-Q5_K_M (3.43 s)

TRANSLATION: I'm so busy I'd take help from a cat, so please give me a hand.
LITERAL: Cat's hand also want to borrow extent busy am, help give.
NOTES:
- The speaker is using a casual, slightly pleading tone. They are likely a protagonist or a friend asking for a favor.
- 猫の手も借りたい (neko no te mo karitai) is a common idiom meaning to be extremely busy. It implies being so overwhelmed that you'd accept help from even a cat (which is useless for chores).
- 手伝ってくれ (tetsudatte kure) is a masculine or rough way to say "help me." The suffix くれ is a command/request form often used by men or in informal settings.
- ほど (hodo) is used here to indicate a degree or extent, similar to "to the point that..."

### gemma-4-12b-Q6_K (3.36 s)

TRANSLATION: I'm so busy I'd take help from even a cat. Please give me a hand.
LITERAL: Cat's hand also want to borrow extent busy am, help give me.
NOTES:
- The speaker is likely a stressed person in a position of responsibility, using a standard but slightly informal request.
- 猫の手も借りたい is a common idiom meaning "to be extremely busy." It implies the person is so overwhelmed they would accept help from anyone, even a cat (which obviously can't help).
- ほど is a particle used to indicate a degree or extent (to the extent that...).
- 手伝ってくれ is a casual, slightly masculine/blunt way to say "help me." The くれ ending is a command-like request.
