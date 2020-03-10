package grass.plugins

import grass.GrassMixin
import java.time.LocalDate
import java.time.*
import java.time.ZoneId

import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import groovy.text.SimpleTemplateEngine

import com.google.common.eventbus.Subscribe


class CorePlugin implements GrassMixin {
    def DATE = DateTimeFormatter.ISO_LOCAL_DATE
    def DATETIME = DateTimeFormatter.ISO_DATE //new SimpleDateFormat('yyyy-MM-dd hh:mm')

    def config

    private Closure pageProperty = { p, v ->
        page."$p" = v
    }

    def setupBinding(binding) {
        // add bindings to change metadata
        binding.template = pageProperty.curry('template')
        binding.title = pageProperty.curry('name')
        binding.title = pageProperty.curry('title')
        binding.summary = pageProperty.curry('summary')
        binding.out = pageProperty.curry('out')
        binding.date = { date ->
            if (date instanceof LocalDate) {
                page.date = date
            } else {
                def str = date.toString()
                try {
                    page.date = LocalDate.parse(str, DATETIME)
                } catch (e) {
                    try {
                        page.date = LocalDate.parse(str, DATE)
                    } catch (e2) {
                        // not valid format
                    }
                }
            }
        }
        binding.include = { path ->
            def file = findTemplate(path, binding)
            if (file.exists()) {
                evaluate(file.text, binding)
            }
        }
        binding.render = { Map args ->
            def pathKey = ['path', 'template', 'using'].find { args.containsKey(it) }
            if (!pathKey) fail('Template path required')
            def path = args[pathKey]

            def file = findTemplate(path, binding)

            if (file.exists()) {
                def data = [:]
                data.putAll(binding)

                def varKey = ['var', 'as'].find { args[it] }
                def var = varKey ? args[varKey] : 'it'

                args.model.collect { m ->
                    data[var] = m
                    new SimpleTemplateEngine().createTemplate(file.text).make(data).toString()
                }.join('\n')
            }
        }
        binding.when = { test, out ->
            test ? out : ''
        }
        binding.unless = { test, out ->
            test ? '' : out
        }
        binding.isodate = { date ->
            // TODO
            //date.format(DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("PST", ZoneId.SHORT_IDS)))
        }
        binding.prettydate = { date ->
            date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        }
    }
}
