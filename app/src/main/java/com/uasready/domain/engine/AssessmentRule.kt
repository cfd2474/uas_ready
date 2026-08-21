package com.uasready.domain.engine

import com.uasready.domain.model.AssessmentCategory
import com.uasready.domain.model.CategoryAssessment

/**
 * Interface for domain rule evaluators. Each evaluator operates independently over the context.
 */
interface CategoryRuleEvaluator {
    val category: AssessmentCategory
    fun evaluate(context: AssessmentContext): CategoryAssessment
}
