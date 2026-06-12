package com.example

data class FlashCardItem(
    val front: String,
    val back: String
)

data class QuizQuestionItem(
    val question: String,
    val options: List<String> = emptyList(), // Empty if Fill-In question
    val correctAnswer: String, // For MCQs, exact string matching an option. For Fill-In, the core keyword or exact string.
    val isFillIn: Boolean = false,
    val hint: String = ""
)

data class StudyTopic(
    val title: String,
    val summary: String,
    val flashcards: List<FlashCardItem>,
    val quizQuestions: List<QuizQuestionItem>
)

data class SubjectData(
    val name: String,
    val category: String, // "Mathematics", "Physical Sciences", "Life Sciences", "Agricultural Sciences"
    val grade: Int, // 11 or 12
    val topics: List<StudyTopic>
)

object StaticContent {
    val subjectsList = listOf(
        // ================= MATHEMATICS GRADE 11 =================
        SubjectData(
            name = "Mathematics (Grade 11)",
            category = "Mathematics",
            grade = 11,
            topics = listOf(
                StudyTopic(
                    title = "Quadratic Equations & Roots",
                    summary = "In Grade 11 Mathematics, Quadratic Equations are solved using factoring, completing the square, or the quadratic formula: x = [-b ± √(b² - 4ac)] / 2a. The nature of roots is determined by the discriminant, Delta (Δ = b² - 4ac). If Δ > 0: roots are real and unequal. If Δ = 0: roots are real and equal. If Δ < 0: roots are non-real (imaginary). If Δ is a perfect square: roots are rational, otherwise they are irrational.",
                    flashcards = listOf(
                        FlashCardItem("What is the quadratic formula?", "x = [-b \u00B1 \u221A(b\u00B2 - 4ac)] / 2a"),
                        FlashCardItem("What does the discriminant (Delta) determine?", "The nature of the roots of a quadratic equation (whether real, equal, or non-real)."),
                        FlashCardItem("What is the nature of roots if Delta is negative (Delta < 0)?", "Roots are non-real / imaginary."),
                        FlashCardItem("If Delta is a perfect square, what type of roots do we get?", "Rational roots.")
                    ),
                    quizQuestions = listOf(
                        QuizQuestionItem(
                            question = "If the discriminant b\u00B2 - 4ac is equal to 0, what is the nature of the roots?",
                            options = listOf("Real and equal", "Real and unequal", "Non-real", "Irrational"),
                            correctAnswer = "Real and equal",
                            hint = "Think about whether you add or subtract zero in the formula."
                        ),
                        QuizQuestionItem(
                            question = "What is the formula for the discriminant?",
                            options = listOf("b\u00B2 - 4ac", "b\u207B4ac", "\u221A(b\u00B2 - 4ac)", "-b / 2a"),
                            correctAnswer = "b\u00B2 - 4ac",
                            hint = "It's the expression inside the square root of the quadratic formula."
                        ),
                        QuizQuestionItem(
                            question = "Solve x\u00B2 - 5x + 6 = 0. The solutions are x = 2 and x = ...",
                            correctAnswer = "3",
                            isFillIn = true,
                            hint = "Factorize the quadratic expression into (x - 2)(x - 3) = 0."
                        )
                    )
                ),
                StudyTopic(
                    title = "Trigonometric Sine & Cosine Rules",
                    summary = "The Sine and Cosine rules let us solve non-right-angled triangles. The Sine Rule states: a/sin(A) = b/sin(B) = c/sin(C). Use it when we have an angle & opposite side, plus another side or angle. The Cosine Rule states: a\u00B2 = b\u00B2 + c\u00B2 - 2bc*cos(A). Use it when we have SAS (Side-Angle-Side) or SSS (Side-Side-Side). The Area of triangle is: Area = 0.5*a*b*sin(C).",
                    flashcards = listOf(
                        FlashCardItem("State the Sine Rule formula", "a / sin(A) = b / sin(B) = c / sin(C)"),
                        FlashCardItem("State the Cosine Rule formula for side 'a'", "a\u00B2 = b\u00B2 + c\u00B2 - 2bc\u22C5cos(A)"),
                        FlashCardItem("When do we use the Cosine Rule?", "When we are given SAS (Side-Angle-Side) or SSS (Side-Side-Side)."),
                        FlashCardItem("What is the general area formula of a non-right-angled triangle?", "Area = 1/2 \u22C5 a \u22C5 b \u22C5 sin(C)")
                    ),
                    quizQuestions = listOf(
                        QuizQuestionItem(
                            question = "Which rule should you use if given all three side lengths (SSS) of a triangle?",
                            options = listOf("Cosine Rule", "Sine Rule", "Pythagoras Theorem", "Tangent Rule"),
                            correctAnswer = "Cosine Rule",
                            hint = "We don't have any angles initially, so we need a rule grouping all three sides and one angle."
                        ),
                        QuizQuestionItem(
                            question = "What is the Area of a triangle ABC if a = 6, b = 8 and angle C = 30 degrees?",
                            options = listOf("12", "24", "16", "48"),
                            correctAnswer = "12",
                            hint = "Formula: Area = 0.5 * 6 * 8 * sin(30). Note that sin(30\u00B0) = 0.5."
                        ),
                        QuizQuestionItem(
                            question = "The Sine Rule formula is a / ... = b / sin(B). (Type the missing divisor)",
                            correctAnswer = "sin(A)",
                            isFillIn = true,
                            hint = "It is proportional to the sine of the angle opposite to side a."
                        )
                    )
                )
            )
        ),

        // ================= MATHEMATICS GRADE 12 =================
        SubjectData(
            name = "Mathematics (Grade 12)",
            category = "Mathematics",
            grade = 12,
            topics = listOf(
                StudyTopic(
                    title = "Differential Calculus",
                    summary = "Grade 12 Calculus focuses on limits, rates of change, and finding derivatives from first principles or rules. First principles formula: f'(x) = lim(h->0) [f(x+h) - f(x)] / h. Power rule: d/dx(x^n) = n*x^(n-1). Applications include finding equation of tangent to curve at a point, curve sketching (stationary points where f'(x) = 0, inflection points where f''(x) = 0), and optimization word problems.",
                    flashcards = listOf(
                        FlashCardItem("What is the calculus formula from first principles?", "f'(x) = lim(h->0) [f(x+h) - f(x)] / h"),
                        FlashCardItem("Derivative of x\u00B3 using power rule?", "3x\u00B2"),
                        FlashCardItem("What is true at a stationary/turning point of a function?", "The first derivative of the function is zero: f'(x) = 0."),
                        FlashCardItem("How do you find the point of inflection?", "By setting the second derivative to zero: f''(x) = 0.")
                    ),
                    quizQuestions = listOf(
                        QuizQuestionItem(
                            question = "Find the derivative of f(x) = 4x\u00B3 + 2x with respect to x.",
                            options = listOf("12x\u00B2 + 2", "12x\u00B3 + 2x", "4x\u00B2 + 2", "12x"),
                            correctAnswer = "12x\u00B2 + 2",
                            hint = "Bring down the exponents and subtract one from them."
                        ),
                        QuizQuestionItem(
                            question = "At what point does a curve have a stationary turning point?",
                            options = listOf("Where f'(x) = 0", "Where f(x) = 0", "Where f''(x) = 0", "Where x = 0"),
                            correctAnswer = "Where f'(x) = 0",
                            hint = "Turning points have flat horizontal tangent lines, so the slope/gradient is zero."
                        ),
                        QuizQuestionItem(
                            question = "Use the power rule to find the derivative of 5x. f'(x) = ...",
                            correctAnswer = "5",
                            isFillIn = true,
                            hint = "Derivative of x with respect to x is 1."
                        )
                    )
                )
            )
        ),

        // ================= PHYSICAL SCIENCES GRADE 11 =================
        SubjectData(
            name = "Physical Sciences (Grade 11)",
            category = "Physical Sciences",
            grade = 11,
            topics = listOf(
                StudyTopic(
                    title = "Newton's Laws of Motion",
                    summary = "Grade 11 Physics mechanics lies on Newton's laws. Newton's 1st Law (Inertia): An object continues in a state of rest or uniform motion in a straight line unless acted upon by a net force. Newton's 2nd Law: When a net force acts on an object, it accelerates in the direction of the net force, where F_net = m*a. Newton's 3rd Law: When object A exerts a force on B, B simultaneously exerts an equal and opposite force on A. Newton's Law of Universal Gravitation: F = G*(m1*m2)/r².",
                    flashcards = listOf(
                        FlashCardItem("State Newton's Second Law in equation form", "F_net = m \u22C5 a"),
                        FlashCardItem("Define Inertia", "The resistance of an object to any change in its state of rest or motion."),
                        FlashCardItem("What is the difference between mass and weight?", "Mass is the amount of matter in an object (kg, constant). Weight is the gravitational force acting on it (N, changes with position)."),
                        FlashCardItem("Universal Gravitation equation?", "F = G \u22C5 (m1 \u22C5 m2) / r\u00B2")
                    ),
                    quizQuestions = listOf(
                        QuizQuestionItem(
                            question = "If the net force acting on an object is doubled while its mass is kept constant, what happens to its acceleration?",
                            options = listOf("Doubled", "Halved", "Four times greater", "Remains unchanged"),
                            correctAnswer = "Doubled",
                            hint = "F_net is directly proportional to acceleration (F = ma)."
                        ),
                        QuizQuestionItem(
                            question = "Which force always opposes the motion of an object along a surface?",
                            options = listOf("Frictional force", "Normal force", "Gravitational force", "Applied force"),
                            correctAnswer = "Frictional force",
                            hint = "It's a contact force parallel to the surface."
                        ),
                        QuizQuestionItem(
                            question = "What is the unit of force? (Type the name, e.g. Joule, Newton, Watt)",
                            correctAnswer = "Newton",
                            isFillIn = true,
                            hint = "Named after the scientist Isaac who developed these three motion laws."
                        )
                    )
                )
            )
        ),

        // ================= PHYSICAL SCIENCES GRADE 12 =================
        SubjectData(
            name = "Physical Sciences (Grade 12)",
            category = "Physical Sciences",
            grade = 12,
            topics = listOf(
                StudyTopic(
                    title = "Doppler Effect (Waves & Sound)",
                    summary = "The Doppler Effect is the change in frequency (or pitch) of a wave detected by a listener, because the sound source and the listener are moving relative to each other. Formula: f_L = [(v ± v_L) / (v ∓ v_s)] * f_s, where v is speed of sound, v_L is listener speed, v_s is source speed, f_L is listener frequency, f_s is source frequency. As source approaches listener: pitch increases (f_L > f_s). As source recedes: pitch decreases (f_L < f_s). Applications: Red shift in astronomy (proves universe expansion), Doppler flow meter in medicine.",
                    flashcards = listOf(
                        FlashCardItem("Define the Doppler Effect", "The apparent change in frequency of a wave due to relative motion between source and observer."),
                        FlashCardItem("What happens to frequency as a sound source approaches you?", "It appears to increase (higher pitch)."),
                        FlashCardItem("What happens to frequency as a sound source moves away from you?", "It appears to decrease (lower pitch)."),
                        FlashCardItem("State a real-world medical application of the Doppler Effect", "Doppler Ultrasound / Flow Meter to measure blood flow velocity.")
                    ),
                    quizQuestions = listOf(
                        QuizQuestionItem(
                            question = "When an ambulance moving with its siren on approaches a stationary observer, the observer hears a frequency that is...",
                            options = listOf("Higher than the source frequency", "Lower than the source frequency", "Equal to the source frequency", "Alternating high and low"),
                            correctAnswer = "Higher than the source frequency",
                            hint = "Sound wave crests are compressed together as the vehicle catches up to its own waves."
                        ),
                        QuizQuestionItem(
                            question = "What astronomical discovery is supported by the Doppler Effect of light waves?",
                            options = listOf("The universe is expanding", "The Earth is round", "Black holes exist", "Stars are composed of Hydrogen"),
                            correctAnswer = "The universe is expanding",
                            hint = "Related to 'Red shift' of distant galaxies moving away from us."
                        ),
                        QuizQuestionItem(
                            question = "Type 'Red' or 'Blue' - Which shift of light waves from stars indicates they are moving away from Earth?",
                            correctAnswer = "Red",
                            isFillIn = true,
                            hint = "Moving away means longer wavelengths, which are towards the red end of the spectrum."
                        )
                    )
                )
            )
        ),

        // ================= LIFE SCIENCES GRADE 11 =================
        SubjectData(
            name = "Life Sciences (Grade 11)",
            category = "Life Sciences",
            grade = 11,
            topics = listOf(
                StudyTopic(
                    title = "Cellular Respiration",
                    summary = "Cellular Respiration is the process where cells break down glucose to release energy (ATP). It occurs in the cytoplasm and mitochondria. Anaerobic Respiration: requires no Oxygen (Glycolysis only, produces lactic acid in animals or ethanol + CO2 in yeast). Aerobic Respiration: requires Oxygen. It occurs in three stages: 1. Glycolysis (cytoplasm), 2. Krebs Cycle (mitochondrial matrix), 3. Oxidative Phosphorylation (mitochondrial cristae). Aerobic respiration produces 36-38 ATP per glucose molecule.",
                    flashcards = listOf(
                        FlashCardItem("What is the primary product of cellular respiration used for cellular energy?", "Adenosine Triphosphate (ATP)"),
                        FlashCardItem("Where in the cell does Aerobic Respiration take place?", "In the cytoplasm (Glycolysis) and Mitochondria (Krebs cycle & Oxidative Phosphorylation)."),
                        FlashCardItem("What are the inputs / active reactants of Aerobic Respiration?", "Glucose and Oxygen."),
                        FlashCardItem("What are the final waste products of plant respiration?", "Carbon Dioxide (CO\u2082) and Water (H\u2082O).")
                    ),
                    quizQuestions = listOf(
                        QuizQuestionItem(
                            question = "In which specific part of the cell does Glycolysis occur?",
                            options = listOf("Cytoplasm", "Mitochondrial matrix", "Mitochondrial cristae", "Ribosomes"),
                            correctAnswer = "Cytoplasm",
                            hint = "It's the jelly-like fluid phase of the cell."
                        ),
                        QuizQuestionItem(
                            question = "How many ATP molecules are roughly produced in total per glucose molecule during aerobic cellular respiration?",
                            options = listOf("2", "4", "36", "120"),
                            correctAnswer = "36",
                            hint = "It's significantly higher than anaerobic respiration, which yields only 2 ATP."
                        ),
                        QuizQuestionItem(
                            question = "What gas is required as an electron acceptor at the end of aerobic respiration?",
                            correctAnswer = "Oxygen",
                            isFillIn = true,
                            hint = "The air we breathe in to survive!"
                        )
                    )
                )
            )
        ),

        // ================= LIFE SCIENCES GRADE 12 =================
        SubjectData(
            name = "Life Sciences (Grade 12)",
            category = "Life Sciences",
            grade = 12,
            topics = listOf(
                StudyTopic(
                    title = "DNA & Protein Synthesis",
                    summary = "DNA (Deoxyribonucleic Acid) carries hereditary instructions. It is a double helix made of nucleotides (Phosphate, Deoxyribose sugar, Nitrogenous bases: Adenine, Thymine, Guanine, Cytosine). RNA (Ribonucleic Acid) is single stranded, has Ribose sugar and Uracil instead of Thymine. Protein Synthesis involves: 1. Transcription (DNA code copied to mRNA in nucleus), 2. Translation (mRNA code read by ribosomes in cytoplasm, tRNA bring specific amino acids to form polypeptide chain/protein).",
                    flashcards = listOf(
                        FlashCardItem("What are the nitrogenous bases of DNA?", "Adenine (A), Thymine (T), Guanine (G), Cytosine (C)"),
                        FlashCardItem("Which base replaces Thymine in RNA?", "Uracil (U)"),
                        FlashCardItem("Where does Transcription occur?", "In the Nucleus."),
                        FlashCardItem("What is a codon?", "A sequence of three nucleotides on mRNA that codes for a specific amino acid.")
                    ),
                    quizQuestions = listOf(
                        QuizQuestionItem(
                            question = "Which enzyme is responsible for forming mRNA during Transcription?",
                            options = listOf("RNA Polymerase", "DNA Polymerase", "Helicase", "Amylase"),
                            correctAnswer = "RNA Polymerase",
                            hint = "It synthesizes an RNA polymer based on a DNA template."
                        ),
                        QuizQuestionItem(
                            question = "What base pairs with Adenine (A) in a DNA double helix?",
                            options = listOf("Thymine", "Uracil", "Guanine", "Cytosine"),
                            correctAnswer = "Thymine",
                            hint = "In RNA it pairs with Uracil, but in DNA it bonds with T."
                        ),
                        QuizQuestionItem(
                            question = "Where in the cytoplasm does Translation (protein assembly) occur?",
                            correctAnswer = "Ribosome",
                            isFillIn = true,
                            hint = "The tiny protein-making factories of the cell."
                        )
                    )
                )
            )
        ),

        // ================= AGRICULTURAL SCIENCES GRADE 11 =================
        SubjectData(
            name = "Agricultural Sciences (Grade 11)",
            category = "Agricultural Sciences",
            grade = 11,
            topics = listOf(
                StudyTopic(
                    title = "Soil Texture & Chemistry",
                    summary = "Soil Science is essential in Agriculture. Soil Texture is the relative proportion of sand, silt, and clay particles in soil. Clay soils have tiny pore spaces, hold water tightly, and have high CEC (Cation Exchange Capacity) but poor aeration. Sandy soils are well aerated but drain quickly and hold few nutrients. Soil pH affects nutrient availability. South African acidic soils (pH < 5.5) are often treated with agricultural lime (Calcium Carbonate) to neutralize acidity and improve crop yields.",
                    flashcards = listOf(
                        FlashCardItem("What are the three main mineral components of soil texture?", "Sand, Silt, and Clay"),
                        FlashCardItem("Which soil type has the highest water retention and nutrient holding capacity?", "Clay soil"),
                        FlashCardItem("What material is added to acidic soils to increase pH?", "Agricultural Lime (Calcium Carbonate / Calcite)"),
                        FlashCardItem("What is CEC in soil science?", "Cation Exchange Capacity - the soil's ability to hold and exchange mineral nutrients.")
                    ),
                    quizQuestions = listOf(
                        QuizQuestionItem(
                            question = "Which soil texture type has the largest average particle size?",
                            options = listOf("Sand", "Silt", "Clay", "Loam"),
                            correctAnswer = "Sand",
                            hint = "Its grains are easily visible to the naked eye."
                        ),
                        QuizQuestionItem(
                            question = "What chemical compound is most commonly used in agricultural lime to raise soil pH?",
                            options = listOf("Calcium Carbonate", "Sodium Chloride", "Ammonium Nitrate", "Copper Sulphate"),
                            correctAnswer = "Calcium Carbonate",
                            hint = "It's the chemical composition of limestone."
                        ),
                        QuizQuestionItem(
                            question = "What is the term for decaying organic matter in soil that improves fertility? (Hum...)",
                            correctAnswer = "Humus",
                            isFillIn = true,
                            hint = "Dark, spongy organic substance derived from leaf litter and roots decomposition."
                        )
                    )
                )
            )
        ),

        // ================= AGRICULTURAL SCIENCES GRADE 12 =================
        SubjectData(
            name = "Agricultural Sciences (Grade 12)",
            category = "Agricultural Sciences",
            grade = 12,
            topics = listOf(
                StudyTopic(
                    title = "Animal Nutrition & Digestion",
                    summary = "Grade 12 Agricultural Science looks at farm animal digestion. Large ruminant animals (cattle, sheep) are polygastric and have 4 stomach compartments: Rumen (fermentation vat with microbes), Reticulum (honeycomb filter), Omasum (water absorption), and Abomasum (true glandular stomach). Non-ruminant animals (pigs, chickens) are monogastric and have a single stomach. Feeds are classified into Roughages (high fiber, low energy, e.g. hay) and Concentrates (low fiber, high digestibility and energy, e.g. maize meal).",
                    flashcards = listOf(
                        FlashCardItem("Name the four compartments of a ruminant's stomach", "Rumen, Reticulum, Omasum, Abomasum"),
                        FlashCardItem("Which compartment is known as the 'true stomach'?", "Abomasum (where acid and enzymes digest food)"),
                        FlashCardItem("What is the difference between roughages and concentrates?", "Roughages are high fiber/low energy (e.g. hay). Concentrates are low fiber/high energy (e.g. maize meal)."),
                        FlashCardItem("Where does microbial fermentation occur in ruminants?", "In the Rumen.")
                    ),
                    quizQuestions = listOf(
                        QuizQuestionItem(
                            question = "Which of the following is the true glandular stomach of a ruminant?",
                            options = listOf("Abomasum", "Rumen", "Reticulum", "Omasum"),
                            correctAnswer = "Abomasum",
                            hint = "It works just like the stomach of a human or monogastric pig."
                        ),
                        QuizQuestionItem(
                            question = "What category of feed has high crude fiber content (>18%) and low energy density?",
                            options = listOf("Roughage", "Concentrate", "Mineral Lick", "Additive"),
                            correctAnswer = "Roughage",
                            hint = "Examples include grass, silage, and straw."
                        ),
                        QuizQuestionItem(
                            question = "What is the honeycomb-like compartment of a ruminant stomach?",
                            correctAnswer = "Reticulum",
                            isFillIn = true,
                            hint = "Stops heavy objects from proceeding further into the gut."
                        )
                    )
                )
            )
        )
    )
}
