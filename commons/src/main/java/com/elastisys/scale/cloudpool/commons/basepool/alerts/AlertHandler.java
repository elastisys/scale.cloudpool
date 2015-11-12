package com.elastisys.scale.cloudpool.commons.basepool.alerts;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.commons.basepool.config.AlertsConfig;
import com.elastisys.scale.cloudpool.commons.basepool.config.TimeInterval;
import com.elastisys.scale.commons.net.alerter.Alert;
import com.elastisys.scale.commons.net.alerter.Alerter;
import com.elastisys.scale.commons.net.alerter.filtering.FilteringAlerter;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerter;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerterConfig;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerter;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerterConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.gson.JsonElement;

/**
 * Listens to an {@link EventBus} and dispatches any incoming {@link Alert}s to
 * its registered {@link Alerter}s.
 */
public class AlertHandler {
	private final static Logger LOG = LoggerFactory
			.getLogger(AlertHandler.class);
	/**
	 * {@link EventBus} used to post {@link Alert} events that are to be
	 * forwarded by configured {@link Alerter}s (if any).
	 */
	private final EventBus eventBus;

	/**
	 * Holds the list of configured {@link Alerter}s (if any). Each
	 * {@link Alerter} is registered with the {@link EventBus} to forward posted
	 * {@link Alert}s.
	 */
	private List<Alerter> alerters;

	/**
	 * Creates an {@link AlertHandler} that listens to {@link Alert}s on the
	 * given {@link EventBus} and dispatches them to any registered
	 * {@link Alerter}s.
	 *
	 * @param eventBus
	 *            The {@link EventBus} on which to listen for {@link Alert}s.
	 */
	public AlertHandler(EventBus eventBus) {
		checkArgument(eventBus != null, "no eventBus given");
		this.eventBus = eventBus;
		this.alerters = new CopyOnWriteArrayList<>();
	}

	/**
	 * Registers {@link Alerter}s with the {@link EventBus}.
	 *
	 * @param alertsConfig
	 *            The {@link Alerter}s to register. May be <code>null</code>,
	 *            meaning no {@link Alerter}s will be registered.
	 * @param standardAlertMetadataTags
	 *            Tags that are to be included in all sent out {@link Alert}s
	 *            (in addition to those already set on the {@link Alert}
	 *            itself). May be <code>null</code>, which means no standard
	 *            tags are to be used.
	 */
	public void registerAlerters(AlertsConfig alertsConfig,
			Map<String, JsonElement> standardAlertMetadataTags) {
		if (alertsConfig == null) {
			LOG.debug("no alert handlers registered.");
			return;
		}
		Map<String, JsonElement> standardTags = ImmutableMap.of();
		if (standardAlertMetadataTags != null) {
			standardTags = standardAlertMetadataTags;
		}

		List<Alerter> newAlerters = Lists.newArrayList();
		// add SMTP alerters
		List<SmtpAlerterConfig> smtpAlerters = alertsConfig.getSmtpAlerters();
		LOG.debug("adding {} SMTP alerter(s)", smtpAlerters.size());
		for (SmtpAlerterConfig smtpAlerterConfig : smtpAlerters) {
			newAlerters.add(filteredAlerter(
					new SmtpAlerter(smtpAlerterConfig, standardTags),
					alertsConfig.getDuplicateSuppression()));
		}
		// add HTTP alerters
		List<HttpAlerterConfig> httpAlerters = alertsConfig.getHttpAlerters();
		LOG.debug("adding {} HTTP alerter(s)", httpAlerters.size());
		for (HttpAlerterConfig httpAlerterConfig : httpAlerters) {
			newAlerters.add(filteredAlerter(
					new HttpAlerter(httpAlerterConfig, standardTags),
					alertsConfig.getDuplicateSuppression()));
		}
		// register every alerter with event bus
		for (Alerter alerter : newAlerters) {
			this.eventBus.register(alerter);
		}
		this.alerters.addAll(newAlerters);
	}

	private Alerter filteredAlerter(Alerter alerter,
			TimeInterval duplicateSuppression) {
		LOG.debug("alert duplicate suppression: {}", duplicateSuppression);
		long suppressionTime = duplicateSuppression.getTime();
		TimeUnit timeUnit = duplicateSuppression.getUnit();
		return new FilteringAlerter(alerter, suppressionTime, timeUnit);
	}

	/**
	 * Unregisters any configured {@link Alerter}s from the {@link EventBus}.
	 */
	public void unregisterAlerters() {
		this.alerters.stream()
				.forEach(alerter -> this.eventBus.unregister(alerter));
		this.alerters.clear();
	}

	/**
	 * Returns <code>true</code> if this {@link AlertHandler} has no registered
	 * {@link Alerter}s, <code>false</code> otherwise.
	 *
	 * @return
	 */
	public synchronized boolean isEmpty() {
		return this.alerters.isEmpty();
	}

	/**
	 * Return a copy of the currently configured {@link Alerter}s.
	 *
	 * @return
	 */
	List<Alerter> alerters() {
		return ImmutableList.copyOf(this.alerters);
	}

}
